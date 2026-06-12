import { useEffect, useState } from 'react';
import { api } from '../services/apiClient';
import { Layout } from '../components/Layout';
import { Plus, Edit2, Trash2, Save, X } from 'lucide-react';

interface Product {
    id: string;
    name: string;
    price: number;
    category: string;
    emoji: string;
    in_stock: boolean; // boolean stored as 1/0
}

const INITIAL_PRODUCTS = [
    { name: "Bocadillo Jamón", price: 6.50, category: "Comida", emoji: "🥪", in_stock: true },
    { name: "Cerveza Estrella", price: 5.00, category: "Bebidas", emoji: "🍺", in_stock: true },
    { name: "Hot Dog", price: 5.50, category: "Comida", emoji: "🌭", in_stock: true },
    { name: "Agua 500ml", price: 2.50, category: "Bebidas", emoji: "💧", in_stock: true },
    { name: "Nachos con Queso", price: 7.00, category: "Comida", emoji: "🧀", in_stock: true },
    { name: "Coca-Cola", price: 3.50, category: "Bebidas", emoji: "🥤", in_stock: true },
    { name: "Gorra Oficial", price: 35.00, category: "Merch", emoji: "🧢", in_stock: true },
    { name: "Camiseta Equipo", price: 45.00, category: "Merch", emoji: "👕", in_stock: true }
];

const ProductsPage = () => {
    const [products, setProducts] = useState<Product[]>([]);
    const [editMode, setEditMode] = useState<string | null>(null);
    const [editedProduct, setEditedProduct] = useState<Partial<Product>>({});

    useEffect(() => {
        fetchProducts();
    }, []);

    const fetchProducts = async () => {
        try {
            const data = await api.get<Product>('products');
            if (data.length === 0) {
                // Auto-seed for first run
                await seedProducts();
            } else {
                setProducts(data);
            }
        } catch (e) {
            console.error(e);
        }
    };

    const seedProducts = async () => {
        for (const p of INITIAL_PRODUCTS) {
            await api.upsert('products', { ...p, id: crypto.randomUUID() });
        }
        const data = await api.get<Product>('products');
        setProducts(data);
    };

    const handleSave = async () => {
        if (!editedProduct.name || !editedProduct.price) return;

        try {
            const id = editMode === 'new' ? crypto.randomUUID() : editMode;
            await api.upsert('products', {
                id,
                ...editedProduct,
                price: Number(editedProduct.price),
                in_stock: editedProduct.in_stock ? 1 : 0
            });

            setEditMode(null);
            setEditedProduct({});
            fetchProducts();
        } catch (e) {
            console.error(e);
            alert('Error al guardar');
        }
    };

    const handleDelete = async (id: string) => {
        if (!confirm('¿Eliminar producto?')) return;
        try {
            await api.delete('products', { id });
            setProducts(prev => prev.filter(p => p.id !== id));
        } catch (e) {
            console.error(e);
        }
    };

    return (
        <Layout>
            <div className="p-8 max-w-7xl mx-auto">
                <div className="flex justify-between items-center mb-8">
                    <div>
                        <h1 className="text-4xl font-black text-white italic tracking-tighter">GESTIÓN MENU</h1>
                        <p className="text-gray-400 mt-2">Controla el stock y precios en tiempo real</p>
                    </div>
                    <button
                        onClick={() => {
                            setEditMode('new');
                            setEditedProduct({ in_stock: true, category: 'Comida', emoji: '🍔' });
                        }}
                        className="bg-red-600 hover:bg-red-500 text-white px-6 py-3 rounded-xl font-bold flex items-center gap-2"
                    >
                        <Plus size={20} />
                        NUEVO PRODUCTO
                    </button>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {/* Editor Card (New) */}
                    {editMode === 'new' && (
                        <ProductEditor
                            product={editedProduct}
                            onChange={setEditedProduct}
                            onSave={handleSave}
                            onCancel={() => setEditMode(null)}
                        />
                    )}

                    {products.map(product => (
                        editMode === product.id ? (
                            <ProductEditor
                                key={product.id}
                                product={editedProduct}
                                onChange={setEditedProduct}
                                onSave={handleSave}
                                onCancel={() => setEditMode(null)}
                            />
                        ) : (
                            <div key={product.id} className={`p-6 rounded-2xl border ${product.in_stock ? 'bg-gray-900/50 border-gray-800' : 'bg-red-900/20 border-red-900/50'} backdrop-blur-sm relative group`}>
                                <div className="absolute top-4 right-4 flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                    <button
                                        onClick={() => {
                                            setEditMode(product.id);
                                            setEditedProduct({ ...product, in_stock: Boolean(product.in_stock) });
                                        }}
                                        className="p-2 bg-gray-800 rounded-lg hover:bg-gray-700"
                                    >
                                        <Edit2 size={16} />
                                    </button>
                                    <button
                                        onClick={() => handleDelete(product.id)}
                                        className="p-2 bg-gray-800 rounded-lg hover:bg-red-900/50 text-red-500"
                                    >
                                        <Trash2 size={16} />
                                    </button>
                                </div>

                                <div className="flex justify-between items-start mb-4">
                                    <div className="w-16 h-16 rounded-xl bg-gray-800 flex items-center justify-center text-4xl">
                                        {product.emoji}
                                    </div>
                                    <div className={`px-3 py-1 rounded-full text-xs font-bold ${product.in_stock ? 'bg-green-500/20 text-green-400' : 'bg-red-500/20 text-red-400'}`}>
                                        {product.in_stock ? 'EN STOCK' : 'AGOTADO'}
                                    </div>
                                </div>

                                <h3 className="text-xl font-bold text-white mb-1">{product.name}</h3>
                                <p className="text-gray-500 text-sm mb-4">{product.category}</p>
                                <div className="text-2xl font-black text-red-500">€{Number(product.price).toFixed(2)}</div>
                            </div>
                        )
                    ))}
                </div>
            </div>
        </Layout>
    );
};

const ProductEditor = ({ product, onChange, onSave, onCancel }: any) => (
    <div className="p-6 rounded-2xl bg-gray-800 border border-gray-700">
        <div className="flex justify-between mb-4">
            <h3 className="font-bold text-white">Editar Producto</h3>
            <button onClick={onCancel}><X size={20} className="text-gray-400" /></button>
        </div>
        <div className="space-y-4">
            <div>
                <label className="text-xs text-gray-500 block mb-1">Nombre</label>
                <input
                    className="w-full bg-gray-900 border border-gray-700 rounded-lg p-2 text-white"
                    value={product.name || ''}
                    onChange={e => onChange({ ...product, name: e.target.value })}
                />
            </div>
            <div className="flex gap-4">
                <div className="flex-1">
                    <label className="text-xs text-gray-500 block mb-1">Precio (€)</label>
                    <input
                        type="number"
                        className="w-full bg-gray-900 border border-gray-700 rounded-lg p-2 text-white"
                        value={product.price || ''}
                        onChange={e => onChange({ ...product, price: e.target.value })}
                    />
                </div>
                <div className="w-20">
                    <label className="text-xs text-gray-500 block mb-1">Emoji</label>
                    <input
                        className="w-full bg-gray-900 border border-gray-700 rounded-lg p-2 text-white text-center"
                        value={product.emoji || ''}
                        onChange={e => onChange({ ...product, emoji: e.target.value })}
                    />
                </div>
            </div>
            <div className="flex items-center gap-2">
                <input
                    type="checkbox"
                    checked={product.in_stock}
                    onChange={e => onChange({ ...product, in_stock: e.target.checked })}
                    className="w-5 h-5 rounded bg-gray-900 border-gray-700"
                />
                <span className="text-white">Disponible en Stock</span>
            </div>
            <button
                onClick={onSave}
                className="w-full bg-green-600 hover:bg-green-500 text-white py-3 rounded-xl font-bold flex justify-center items-center gap-2 mt-2"
            >
                <Save size={18} />
                GUARDAR
            </button>
        </div>
    </div>
);

export default ProductsPage;
