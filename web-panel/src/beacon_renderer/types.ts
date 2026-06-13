
export enum BeaconMode {
    UNCONFIGURED = 'UNCONFIGURED',
    NORMAL = 'NORMAL',
    CONGESTION = 'CONGESTION',
    EMERGENCY = 'EMERGENCY',
    EVACUATION = 'EVACUATION',
    MAINTENANCE = 'MAINTENANCE'
}

export enum ArrowDirection {
    NONE = 'NONE',
    FORWARD = 'FORWARD',
    BACKWARD = 'BACKWARD',
    LEFT = 'LEFT',
    RIGHT = 'RIGHT',
    FORWARD_LEFT = 'FORWARD_LEFT',
    FORWARD_RIGHT = 'FORWARD_RIGHT',
    BACKWARD_LEFT = 'BACKWARD_LEFT',
    BACKWARD_RIGHT = 'BACKWARD_RIGHT',
    // Directional aliases: callers that think in screen terms (UP/DOWN) resolve
    // to the same wire values as the compass terms (FORWARD/BACKWARD). The
    // shared values are intentional, so the duplicate-value rule is disabled
    // for this block only.
    /* eslint-disable @typescript-eslint/no-duplicate-enum-values */
    UP = 'FORWARD',
    DOWN = 'BACKWARD',
    UP_LEFT = 'FORWARD_LEFT',
    UP_RIGHT = 'FORWARD_RIGHT',
    DOWN_LEFT = 'BACKWARD_LEFT',
    DOWN_RIGHT = 'BACKWARD_RIGHT'
    /* eslint-enable @typescript-eslint/no-duplicate-enum-values */
}

export interface BeaconState {
    mode: BeaconMode;
    arrowDirection: ArrowDirection;
    message: string;
    // ...other props
}

export interface RenderConfig {
    orientation: number; // 0, 90, 180, 270
    width: number;
    height: number;
}
