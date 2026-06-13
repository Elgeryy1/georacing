
export class ArrowComponent {
    private element: HTMLElement;

    constructor() {
        this.element = document.createElement('div');
        this.element.className = 'beacon-arrow';
        // SVG de una flecha estándar
        this.element.innerHTML = `
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="19" x2="12" y2="5"></line>
                <polyline points="5 12 12 5 19 12"></polyline>
            </svg>
        `;

        // Estilos base para asegurar visibilidad
        this.element.style.color = 'white'; // O el color que se pase
        this.element.style.position = 'absolute';
        this.element.style.transformOrigin = 'center center';
    }

    public getElement(): HTMLElement {
        return this.element;
    }

    public updateColor(color: string) {
        this.element.style.color = color;
    }
}
