import { useEffect, useState } from 'react';
import { api } from '../services/apiClient';
import { Layout } from '../components/Layout';
import { Plus, Edit2, Trash2, Save, X, MapPin } from 'lucide-react';

interface FoodStand {
    id: string;
    name: string;
    description: string;
    latitude: number;
    longitude: number;
    zone: string;
    waitMinutes: number;
    rating: number;
    isOpen: boolean;
}

const INITIAL_STANDS: Omit<FoodStand, 'id'>[] = [
    { name: "🍔 Burger Pit", description: "Hamburguesas y patatas", latitude: 41.570, longitude: 2.261, zone: "Tribuna Principal", waitMinutes: 8, rating: 4.5, isOpen: true },
    { name: "🍕 Pizza Box", description: "Pizzas artesanales al horno", latitude: 41.571, longitude: 2.262, zone: "Paddock", waitMinutes: 12, rating: 4.2, isOpen: true },
    { name: "🌮 Taco Stand", description: "Tacos y burritos mexicanos", latitude: 41.569, longitude: 2.260, zone: "Zona Fan", waitMinutes: 5, rating: 4.7, isOpen: true },
    { name: "🍺 Bar Central", description: "Bebidas frías y snacks", latitude: 41.570, longitude: 2.263, zone: "Grada Norte", waitMinutes: 3, rating: 4.0, isOpen: true },
];

const FoodStandsPage = () => {
    const [stands, setStands] = useState<FoodStand[]>([]);
    const [editMode, setEditMode] = useState<string | null>(null);
    const [editedStand, setEditedStand] = useState<Partial<FoodStand>>({});
    const [loading, setLoading] = useState(true);

    useEffect(() => { fetchStands(); }, []);

    const fetchStands = async () => {
        setLoading(true);
        try {
            const data = await api.get<FoodStand>('food_stands');
            if (data.length === 0) {
                await seedStands();
            } else {
                setStands(data);
            }
        } catch (e) {
            console.error(e);
        }
        setLoading(false);
    };

    const seedStands = async () => {
        for (const s of INITIAL_STANDS) {
            await api.upsert('food_stands', { ...s, id: crypto.randomUUID(), isOpen: s.isOpen ? 1 : 0 });
        }
        const data = await api.get<FoodStand>('food_stands');
        setStands(data);
    };

    const handleSave = async () => {
        if (!editedStand.name) return;
        try {
            const id = editMode === 'new' ? crypto.randomUUID() : editMode;
            await api.upsert('food_stands', {
                id,
                ...editedStand,
                latitude: Number(editedStand.latitude || 0),
                longitude: Number(editedStand.longitude || 0),
                waitMinutes: Number(editedStand.waitMinutes || 10),
                rating: Number(editedStand.rating || 4.0),
                isOpen: editedStand.isOpen ? 1 : 0,
            });
            setEditMode(null);
            setEditedStand({});
            fetchStands();
        } catch (e) {
            console.error(e);
            alert('Error al guardar');
        }
    };

    const handleDelete = async (id: string) => {
        if (!confirm('¿Eliminar punto de venta?')) return;
        try {
            await api.delete('food_stands', { id });
            fetchStands();
        } catch (e) {
            console.error(e);
        }
    };

    const toggleOpen = async (stand: FoodStand) => {
        await api.upsert('food_stands', { ...stand, isOpen: stand.isOpen ? 0 : 1 });
        fetchStands();
    };

    return (
        <Layout>
            <div className="p-6">
                <div className="flex justify-between items-center mb-6">
                    <div>
                        <h1 className="text-2xl font-bold text-white">🍽️ Puntos de Venta (Food Stands)</h1>
                        <p className="text-gray-400 text-sm mt-1">Gestiona los puestos de comida del circuito</p>
                    </div>
                    <button
                        onClick={() => { setEditMode('new'); setEditedStand({ isOpen: true, waitMinutes: 10, rating: 4.0 }); }}
                        className="flex items-center gap-2 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700"
                    >
                        <Plus size={16} /> Nuevo Stand
                    </button>
                </div>

                {/* Editor */}
                {editMode && (
                    <div className="bg-gray-800 rounded-xl p-4 mb-6 border border-gray-700">
                        <h3 className="text-white font-bold mb-3">{editMode === 'new' ? 'Nuevo Stand' : 'Editar Stand'}</h3>
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                            <input placeholder="Nombre" value={editedStand.name || ''} onChange={e => setEditedStand({ ...editedStand, name: e.target.value })} className="bg-gray-700 text-white p-2 rounded" />
                            <input placeholder="Descripción" value={editedStand.description || ''} onChange={e => setEditedStand({ ...editedStand, description: e.target.value })} className="bg-gray-700 text-white p-2 rounded" />
                            <input placeholder="Zona" value={editedStand.zone || ''} onChange={e => setEditedStand({ ...editedStand, zone: e.target.value })} className="bg-gray-700 text-white p-2 rounded" />
                            <input type="number" placeholder="Espera (min)" value={editedStand.waitMinutes || ''} onChange={e => setEditedStand({ ...editedStand, waitMinutes: Number(e.target.value) })} className="bg-gray-700 text-white p-2 rounded" />
                            <input type="number" step="0.001" placeholder="Latitud" value={editedStand.latitude || ''} onChange={e => setEditedStand({ ...editedStand, latitude: Number(e.target.value) })} className="bg-gray-700 text-white p-2 rounded" />
                            <input type="number" step="0.001" placeholder="Longitud" value={editedStand.longitude || ''} onChange={e => setEditedStand({ ...editedStand, longitude: Number(e.target.value) })} className="bg-gray-700 text-white p-2 rounded" />
                            <input type="number" step="0.1" placeholder="Rating" value={editedStand.rating || ''} onChange={e => setEditedStand({ ...editedStand, rating: Number(e.target.value) })} className="bg-gray-700 text-white p-2 rounded" />
                            <label className="flex items-center gap-2 text-white">
                                <input type="checkbox" checked={!!editedStand.isOpen} onChange={e => setEditedStand({ ...editedStand, isOpen: e.target.checked })} />
                                Abierto
                            </label>
                        </div>
                        <div className="flex gap-2 mt-3">
                            <button onClick={handleSave} className="flex items-center gap-1 px-3 py-1.5 bg-green-600 text-white rounded hover:bg-green-700"><Save size={14} /> Guardar</button>
                            <button onClick={() => { setEditMode(null); setEditedStand({}); }} className="flex items-center gap-1 px-3 py-1.5 bg-gray-600 text-white rounded hover:bg-gray-500"><X size={14} /> Cancelar</button>
                        </div>
                    </div>
                )}

                {/* Table */}
                <div className="bg-gray-800 rounded-xl overflow-hidden">
                    <table className="w-full text-sm text-left text-gray-300">
                        <thead className="bg-gray-700 text-gray-400">
                            <tr>
                                <th className="px-4 py-3">Nombre</th>
                                <th className="px-4 py-3">Zona</th>
                                <th className="px-4 py-3">Espera</th>
                                <th className="px-4 py-3">Rating</th>
                                <th className="px-4 py-3">Estado</th>
                                <th className="px-4 py-3 text-right">Acciones</th>
                            </tr>
                        </thead>
                        <tbody>
                            {stands.map(s => (
                                <tr key={s.id} className="border-b border-gray-700 hover:bg-gray-750">
                                    <td className="px-4 py-3 font-medium text-white">{s.name}</td>
                                    <td className="px-4 py-3"><span className="flex items-center gap-1"><MapPin size={12} />{s.zone}</span></td>
                                    <td className="px-4 py-3">{s.waitMinutes} min</td>
                                    <td className="px-4 py-3">⭐ {s.rating}</td>
                                    <td className="px-4 py-3">
                                        <button onClick={() => toggleOpen(s)} className={`px-2 py-0.5 rounded text-xs font-bold ${s.isOpen ? 'bg-green-900 text-green-300' : 'bg-red-900 text-red-300'}`}>
                                            {s.isOpen ? 'ABIERTO' : 'CERRADO'}
                                        </button>
                                    </td>
                                    <td className="px-4 py-3 text-right">
                                        <button onClick={() => { setEditMode(s.id); setEditedStand(s); }} className="text-blue-400 hover:text-blue-300 mr-2"><Edit2 size={14} /></button>
                                        <button onClick={() => handleDelete(s.id)} className="text-red-400 hover:text-red-300"><Trash2 size={14} /></button>
                                    </td>
                                </tr>
                            ))}
                            {stands.length === 0 && !loading && (
                                <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-500">No hay stands. Pulsa "Nuevo Stand" para crear uno.</td></tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        </Layout>
    );
};

export default FoodStandsPage;
