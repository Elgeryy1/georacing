using System;
using System.Collections.Concurrent;
using System.Threading.Tasks;
using System.Timers;

namespace BeaconApp.Services
{
    public class ApiLogger
    {
        private readonly ApiClient _apiClient;
        private readonly string _beaconId;
        private readonly ConcurrentQueue<LogEntry> _logQueue;
        private readonly Timer _flushTimer;
        private bool _isInitialized = false;

        public ApiLogger(ApiClient apiClient, string beaconId)
        {
            _apiClient = apiClient;
            _beaconId = beaconId;
            _logQueue = new ConcurrentQueue<LogEntry>();
            
            // Flush logs every 5 seconds
            _flushTimer = new Timer(5000);
            _flushTimer.Elapsed += async (s, e) => await FlushLogsAsync();
            _flushTimer.Start();
        }

        public async Task InitializeAsync()
        {
            if (_isInitialized) return;

            try
            {
                await _apiClient.EnsureTableAsync("beacon_logs");
                await _apiClient.EnsureColumnAsync("beacon_logs", "beacon_uid", "VARCHAR(50)");
                await _apiClient.EnsureColumnAsync("beacon_logs", "level", "VARCHAR(20)");
                await _apiClient.EnsureColumnAsync("beacon_logs", "message", "TEXT");
                await _apiClient.EnsureColumnAsync("beacon_logs", "timestamp", "DATETIME");
                
                _isInitialized = true;
                Log("INFO", "ApiLogger initialized");
            }
            catch (Exception ex)
            {
                FileLogger.LogError("Failed to initialize ApiLogger", ex);
            }
        }

        public void Log(string level, string message)
        {
            // Always log to file first
            FileLogger.Log($"[{level}] {message}");

            // Queue for API
            _logQueue.Enqueue(new LogEntry
            {
                BeaconUid = _beaconId,
                Level = level,
                Message = message,
                Timestamp = DateTime.Now
            });
        }

        private async Task FlushLogsAsync()
        {
            if (!_isInitialized || _logQueue.IsEmpty) return;

            while (_logQueue.TryDequeue(out var entry))
            {
                try
                {
                    // Use _upsert to send log
                    // We generate a unique ID for the log or let the backend handle it if we don't send 'id'
                    // Ideally, we should send an ID if we want to be idempotent, but for logs, fire-and-forget is okay-ish
                    // However, _upsert usually requires a primary key. 
                    // If the backend generates IDs for new rows, we might need to send a unique ID.
                    // Let's assume we generate a GUID for the log ID.
                    
                    var logData = new
                    {
                        id = Guid.NewGuid().ToString(),
                        beacon_uid = entry.BeaconUid,
                        level = entry.Level,
                        message = entry.Message,
                        timestamp = entry.Timestamp.ToString("yyyy-MM-dd HH:mm:ss")
                    };

                    await _apiClient.UpsertAsync("beacon_logs", logData);
                }
                catch (Exception ex)
                {
                    // Don't log to API if API logging fails, just file
                    FileLogger.LogError("Error flushing logs to API", ex);
                }
            }
        }

        private class LogEntry
        {
            public string BeaconUid { get; set; } = string.Empty;
            public string Level { get; set; } = string.Empty;
            public string Message { get; set; } = string.Empty;
            public DateTime Timestamp { get; set; }
        }
    }
}
