import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { api, HttpError } from "./apiClient";

/**
 * Helper: a fetch stub that records every call and returns a queued response.
 */
function mockFetchOnce(impl: () => Promise<Response> | Response) {
  const fn = vi.fn((_input: RequestInfo | URL, _init?: RequestInit) => impl());
  vi.stubGlobal("fetch", fn);
  return fn;
}

function jsonResponse(body: unknown, init?: { status?: number; statusText?: string }) {
  const status = init?.status ?? 200;
  return {
    ok: status >= 200 && status < 300,
    status,
    statusText: init?.statusText ?? "OK",
    json: async () => body,
    text: async () => (typeof body === "string" ? body : JSON.stringify(body)),
  } as unknown as Response;
}

describe("apiClient.request behavior", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    // Silence the console noise the client emits on errors.
    vi.spyOn(console, "error").mockImplementation(() => {});
    vi.spyOn(console, "warn").mockImplementation(() => {});
  });

  afterEach(() => {
    vi.runOnlyPendingTimers();
    vi.useRealTimers();
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it("does NOT retry an HTTP error response (4xx/5xx are thrown immediately)", async () => {
    const fetchFn = mockFetchOnce(() => jsonResponse("bad request", { status: 400, statusText: "Bad Request" }));

    await expect(api.get("beacons")).rejects.toBeInstanceOf(HttpError);
    // A single attempt: HTTP errors must never be retried.
    expect(fetchFn).toHaveBeenCalledTimes(1);
  });

  it("propagates the HTTP status code on the thrown HttpError", async () => {
    mockFetchOnce(() => jsonResponse("nope", { status: 503, statusText: "Service Unavailable" }));

    await expect(api.get("zones")).rejects.toMatchObject({ status: 503 });
  });

  it("retries network failures and eventually succeeds", async () => {
    let calls = 0;
    const fetchFn = vi.fn(async () => {
      calls++;
      if (calls < 3) throw new TypeError("network down");
      return jsonResponse([{ id: 1 }]);
    });
    vi.stubGlobal("fetch", fetchFn);

    const promise = api.get("beacons"); // default retries = 3
    // The client backs off with setTimeout between attempts; advance timers.
    await vi.runAllTimersAsync();
    const result = await promise;

    expect(result).toEqual([{ id: 1 }]);
    expect(fetchFn).toHaveBeenCalledTimes(3);
  });

  it("does NOT retry mutations: upsert performs exactly one fetch on network failure", async () => {
    const fetchFn = vi.fn(async () => {
      throw new TypeError("network down");
    });
    vi.stubGlobal("fetch", fetchFn);

    const promise = api.upsert("beacons", { beacon_uid: "b1" });
    const assertion = expect(promise).rejects.toThrow();
    await vi.runAllTimersAsync();
    await assertion;

    // retries: 0 → a single attempt, no automatic re-send of a write.
    expect(fetchFn).toHaveBeenCalledTimes(1);
  });

  it("does NOT retry mutations: delete performs exactly one fetch on network failure", async () => {
    const fetchFn = vi.fn(async () => {
      throw new TypeError("network down");
    });
    vi.stubGlobal("fetch", fetchFn);

    const promise = api.delete("beacons", { id: 1 });
    const assertion = expect(promise).rejects.toThrow();
    await vi.runAllTimersAsync();
    await assertion;

    expect(fetchFn).toHaveBeenCalledTimes(1);
  });
});

describe("apiClient HTTPS enforcement", () => {
  beforeEach(() => {
    vi.spyOn(console, "error").mockImplementation(() => {});
    vi.spyOn(console, "warn").mockImplementation(() => {});
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.unstubAllEnvs();
    vi.restoreAllMocks();
  });

  it("keeps plain http in dev builds", async () => {
    vi.stubEnv("PROD", false);
    const fetchFn = mockFetchOnce(() => jsonResponse([]));

    await api.get("beacons");

    const calledUrl = String(fetchFn.mock.calls[0]?.[0]);
    expect(calledUrl.startsWith("http://")).toBe(true);
  });

  it("upgrades http to https in production builds", async () => {
    vi.stubEnv("PROD", true);
    const fetchFn = mockFetchOnce(() => jsonResponse([]));

    await api.get("beacons");

    const calledUrl = String(fetchFn.mock.calls[0]?.[0]);
    expect(calledUrl.startsWith("https://")).toBe(true);
    expect(calledUrl.startsWith("http://")).toBe(false);
  });
});
