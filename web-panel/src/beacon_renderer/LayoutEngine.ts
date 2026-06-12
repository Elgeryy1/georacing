
import { ArrowDirection, BeaconState, BeaconMode } from './types';

export class LayoutEngine {

    /**
     * Determines if the arrow should be visible based on state.
     */
    public shouldShowArrow(state: BeaconState): boolean {
        // Regla: Mostrar flecha siempre que haya una dirección definida, en CUALQUIER modo
        return state.arrowDirection !== ArrowDirection.NONE;
    }

    /**
     * Calculates the final rotation in degrees.
     * Rule: finalDirection = arrowDirection + orientation
     */
    public calculateArrowRotation(direction: ArrowDirection, physicalOrientation: number): number {
        const baseAngle = this.directionToDegrees(direction);

        // Sumamos la orientación física
        // (Asumiendo que orientation es rotación horaria en grados)
        let finalAngle = baseAngle + physicalOrientation;

        // Normalizar a 0-360
        finalAngle = finalAngle % 360;
        if (finalAngle < 0) finalAngle += 360;

        return finalAngle;
    }

    private directionToDegrees(direction: ArrowDirection): number {
        switch (direction) {
            case ArrowDirection.FORWARD:
            case ArrowDirection.UP: // Compat
                return 0;
            case ArrowDirection.FORWARD_RIGHT:
            case ArrowDirection.UP_RIGHT: // Compat
                return 45;
            case ArrowDirection.RIGHT:
                return 90;
            case ArrowDirection.BACKWARD_RIGHT:
            case ArrowDirection.DOWN_RIGHT: // Compat
                return 135;
            case ArrowDirection.BACKWARD:
            case ArrowDirection.DOWN: // Compat
                return 180;
            case ArrowDirection.BACKWARD_LEFT:
            case ArrowDirection.DOWN_LEFT: // Compat
                return 225;
            case ArrowDirection.LEFT:
                return 270;
            case ArrowDirection.FORWARD_LEFT:
            case ArrowDirection.UP_LEFT: // Compat
                return 315;
            default: return 0;
        }
    }

    public getArrowStyle(state: BeaconState, physicalOrientation: number) {
        if (!this.shouldShowArrow(state)) {
            return { display: 'none' };
        }

        const rotation = this.calculateArrowRotation(state.arrowDirection, physicalOrientation);

        return {
            display: 'block',
            position: 'absolute',
            top: '50%',
            left: '50%',
            transform: `translate(-50%, -50%) rotate(${rotation}deg)`,
            zIndex: 10, // Layering correcto
            width: '40vmin', // Responsivo
            height: '40vmin'
        };
    }
}
