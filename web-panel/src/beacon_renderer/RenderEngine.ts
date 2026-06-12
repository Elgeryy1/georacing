
import { LayoutEngine } from './LayoutEngine';
import { ArrowComponent } from './components/ArrowComponent';
import { BeaconState } from './types';

export class RenderEngine {
    private container: HTMLElement;
    private layoutEngine: LayoutEngine;
    private arrowComponent: ArrowComponent;
    private physicalOrientation: number = 0; // Configurable

    constructor(containerId: string, orientation: number = 0) {
        const el = document.getElementById(containerId);
        if (!el) throw new Error(`Container ${containerId} not found`);
        this.container = el;
        this.physicalOrientation = orientation;

        this.layoutEngine = new LayoutEngine();
        this.arrowComponent = new ArrowComponent();

        // Mount arrow
        this.container.appendChild(this.arrowComponent.getElement());
    }

    public render(state: BeaconState) {
        // 1. Calcular estilos/layout
        const arrowStyle = this.layoutEngine.getArrowStyle(state, this.physicalOrientation);
        const arrowEl = this.arrowComponent.getElement();

        // 2. Aplicar estilos
        Object.assign(arrowEl.style, arrowStyle);

        // 3. (Opcional) Debugging visual si no se ve
        if (state.mode === 'NORMAL' && arrowStyle.display === 'none') {
            console.warn('[RenderEngine] Arrow hidden in NORMAL mode. Check ArrowDirection.');
        }

        // 4. Otros elementos (texto, fondo) se manejarían aquí...
    }

    public setOrientation(deg: number) {
        this.physicalOrientation = deg;
    }
}
