
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
    // Aliases for compatibility if needed, but UI will use FORWARD/BACKWARD
    UP = 'FORWARD',
    DOWN = 'BACKWARD',
    UP_LEFT = 'FORWARD_LEFT',
    UP_RIGHT = 'FORWARD_RIGHT',
    DOWN_LEFT = 'BACKWARD_LEFT',
    DOWN_RIGHT = 'BACKWARD_RIGHT'
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
