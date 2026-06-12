import { useState, useEffect } from 'react';
import { api } from '../services/apiClient';

export function useCircuitState(intervalMs = 3000) {
    const [state, setState] = useState<any>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        let mounted = true;

        const fetchState = async () => {
            try {
                const data = await api.getCircuitState();
                if (mounted && data) setState(data);
            } catch (err) {
                console.error("Error polling circuit state:", err);
            } finally {
                if (mounted) setLoading(false);
            }
        };

        fetchState(); // Initial call
        const interval = setInterval(fetchState, intervalMs);

        return () => {
            mounted = false;
            clearInterval(interval);
        };
    }, [intervalMs]);

    return { state, loading };
}
