import { useEffect, useState } from 'react';
import { api } from '../services/apiClient';
import { Layout } from '../components/Layout';
import { Plus, Edit2, Trash2, Save, X, Bell } from 'lucide-react';

interface NewsItem {
    id: string;
    title: string;
    content: string;
    timestamp: number;
    category: string;
    priority: string;
}

const CATEGORIES = ['RACE_UPDATE', 'SCHEDULE_CHANGE', 'WEATHER', 'TRAFFIC', 'DRIVER_NEWS', 'SAFETY', 'EVENT', 'GENERAL'];
const PRIORITIES = ['HIGH', 'MEDIUM', 'LOW'];

const PRIORITY_COLORS: Record<string, string> = {
    HIGH: 'bg-red-900 text-red-300',
    MEDIUM: 'bg-yellow-900 text-yellow-300',
    LOW: 'bg-green-900 text-green-300',
};

const CATEGORY_LABELS: Record<string, string> = {
    RACE_UPDATE: '🏁 Carrera',
    SCHEDULE_CHANGE: '📅 Horario',
    WEATHER: '🌤️ Meteorología',
    TRAFFIC: '🚗 Tráfico',
    DRIVER_NEWS: '🏎️ Pilotos',
    SAFETY: '⚠️ Seguridad',
    EVENT: '🎉 Evento',
    GENERAL: '📢 General',
};

const NewsPage = () => {
    const [news, setNews] = useState<NewsItem[]>([]);
    const [editMode, setEditMode] = useState<string | null>(null);
    const [editedNews, setEditedNews] = useState<Partial<NewsItem>>({});
    const [loading, setLoading] = useState(true);

    useEffect(() => { fetchNews(); }, []);

    const fetchNews = async () => {
        setLoading(true);
        try {
            const data = await api.get<NewsItem>('news');
            setNews(data.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0)));
        } catch (e) {
            console.error(e);
        }
        setLoading(false);
    };

    const handleSave = async () => {
        if (!editedNews.title || !editedNews.content) return;
        try {
            const id = editMode === 'new' ? crypto.randomUUID() : editMode;
            const timestamp = editMode === 'new' ? Math.floor(Date.now() / 1000) : (editedNews.timestamp || Math.floor(Date.now() / 1000));
            await api.upsert('news', {
                id,
                title: editedNews.title,
                content: editedNews.content,
                timestamp,
                category: editedNews.category || 'GENERAL',
                priority: editedNews.priority || 'LOW',
            });
            setEditMode(null);
            setEditedNews({});
            fetchNews();
        } catch (e) {
            console.error(e);
            alert('Error al guardar');
        }
    };

    const handleDelete = async (id: string) => {
        if (!confirm('¿Eliminar noticia?')) return;
        try {
            await api.delete('news', { id });
            fetchNews();
        } catch (e) {
            console.error(e);
        }
    };

    const formatTime = (ts: number) => {
        if (!ts) return '—';
        // timestamps stored as Unix seconds; convert to ms for Date
        const d = new Date(ts < 1e12 ? ts * 1000 : ts);
        return d.toLocaleString('es-ES', { day: '2-digit', month: '2-digit', year: '2-digit', hour: '2-digit', minute: '2-digit' });
    };

    return (
        <Layout>
            <div className="p-6">
                <div className="flex justify-between items-center mb-6">
                    <div>
                        <h1 className="text-2xl font-bold text-white">📰 Noticias &amp; Alertas</h1>
                        <p className="text-gray-400 text-sm mt-1">Publica noticias que verán los usuarios en la app</p>
                    </div>
                    <button
                        onClick={() => { setEditMode('new'); setEditedNews({ category: 'GENERAL', priority: 'LOW' }); }}
                        className="flex items-center gap-2 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700"
                    >
                        <Plus size={16} /> Nueva Noticia
                    </button>
                </div>

                {/* Editor */}
                {editMode && (
                    <div className="bg-gray-800 rounded-xl p-4 mb-6 border border-gray-700">
                        <h3 className="text-white font-bold mb-3">{editMode === 'new' ? 'Nueva Noticia' : 'Editar Noticia'}</h3>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                            <input placeholder="Título" value={editedNews.title || ''} onChange={e => setEditedNews({ ...editedNews, title: e.target.value })} className="bg-gray-700 text-white p-2 rounded col-span-2" />
                            <textarea placeholder="Contenido" rows={3} value={editedNews.content || ''} onChange={e => setEditedNews({ ...editedNews, content: e.target.value })} className="bg-gray-700 text-white p-2 rounded col-span-2" />
                            <select value={editedNews.category || 'GENERAL'} onChange={e => setEditedNews({ ...editedNews, category: e.target.value })} className="bg-gray-700 text-white p-2 rounded">
                                {CATEGORIES.map(c => <option key={c} value={c}>{CATEGORY_LABELS[c] || c}</option>)}
                            </select>
                            <select value={editedNews.priority || 'LOW'} onChange={e => setEditedNews({ ...editedNews, priority: e.target.value })} className="bg-gray-700 text-white p-2 rounded">
                                {PRIORITIES.map(p => <option key={p} value={p}>{p === 'HIGH' ? '🔴 Alta' : p === 'MEDIUM' ? '🟡 Media' : '🟢 Baja'}</option>)}
                            </select>
                        </div>
                        <div className="flex gap-2 mt-3">
                            <button onClick={handleSave} className="flex items-center gap-1 px-3 py-1.5 bg-green-600 text-white rounded hover:bg-green-700"><Save size={14} /> Publicar</button>
                            <button onClick={() => { setEditMode(null); setEditedNews({}); }} className="flex items-center gap-1 px-3 py-1.5 bg-gray-600 text-white rounded hover:bg-gray-500"><X size={14} /> Cancelar</button>
                        </div>
                    </div>
                )}

                {/* Lista */}
                <div className="space-y-3">
                    {news.map(n => (
                        <div key={n.id} className="bg-gray-800 rounded-xl p-4 border border-gray-700 flex justify-between items-start">
                            <div className="flex-1">
                                <div className="flex items-center gap-2 mb-1">
                                    <span className={`px-2 py-0.5 rounded text-xs font-bold ${PRIORITY_COLORS[n.priority] || PRIORITY_COLORS.LOW}`}>{n.priority}</span>
                                    <span className="text-gray-500 text-xs">{CATEGORY_LABELS[n.category] || n.category}</span>
                                    <span className="text-gray-600 text-xs ml-auto">{formatTime(n.timestamp)}</span>
                                </div>
                                <h3 className="text-white font-semibold">{n.title}</h3>
                                <p className="text-gray-400 text-sm mt-1">{n.content}</p>
                            </div>
                            <div className="flex gap-1 ml-4">
                                <button onClick={() => { setEditMode(n.id); setEditedNews(n); }} className="text-blue-400 hover:text-blue-300 p-1"><Edit2 size={14} /></button>
                                <button onClick={() => handleDelete(n.id)} className="text-red-400 hover:text-red-300 p-1"><Trash2 size={14} /></button>
                            </div>
                        </div>
                    ))}
                    {news.length === 0 && !loading && (
                        <div className="text-center py-12 text-gray-500">
                            <Bell size={48} className="mx-auto mb-3 opacity-30" />
                            <p>No hay noticias publicadas</p>
                        </div>
                    )}
                </div>
            </div>
        </Layout>
    );
};

export default NewsPage;
