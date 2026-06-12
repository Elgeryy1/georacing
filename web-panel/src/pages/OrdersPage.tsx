import { useEffect, useState } from 'react';
import { api } from '../services/apiClient';
import { Layout } from '../components/Layout';
import { CheckCircle, Clock, ShoppingBag, Trash2, ChefHat, Flame } from 'lucide-react';

interface OrderLine {
    id?: string;
    product_id?: string;
    name?: string;
    price?: number;
    unit_price?: number;
    quantity?: number;
}

interface Order {
    id: string; // Generic DB ID
    order_id: string;
    user_uid: string;
    status: 'PAID' | 'PREPARING' | 'READY' | 'DELIVERED';
    items_json: string; // JSON string
    total_amount: number;
    created_at: string;
}


const OrdersPage = () => {
    const [orders, setOrders] = useState<Order[]>([]);
    const [productsMap, setProductsMap] = useState<Record<string, any>>({});
    const [showHistory, setShowHistory] = useState(false);
    const [loading, setLoading] = useState(true);

    const fetchOrders = async () => {
        try {
            const data = await api.get<Order>('orders');
            const sorted = data.sort((a, b) =>
                new Date(b.created_at).getTime() - new Date(a.created_at).getTime()
            );
            setOrders(sorted);
        } catch (error) {
            console.error("Error fetching orders:", error);
        } finally {
            setLoading(false);
        }
    };

    const fetchProducts = async () => {
        try {
            const list = await api.get<any>('products');
            const map: Record<string, any> = {};
            if (Array.isArray(list)) {
                list.forEach((p: any) => map[p.id] = p);
            }
            setProductsMap(map);
        } catch (e) { console.error(e); }
    };

    useEffect(() => {
        fetchOrders();
        fetchProducts();
        const interval = setInterval(fetchOrders, 5000);
        return () => clearInterval(interval);
    }, []);

    const deleteOrder = async (id: string) => {
        if (!confirm('¿Eliminar pedido?')) return;
        try {
            await api.delete('orders', { id });
            setOrders(prev => prev.filter(o => o.id !== id));
        } catch (e) { console.error(e); }
    };

    const toggleStatus = async (orderId: string, currentStatus: string) => {
        let nextStatus = 'DELIVERED';
        if (currentStatus === 'PAID') nextStatus = 'PREPARING';
        else if (currentStatus === 'PREPARING') nextStatus = 'READY';

        try {
            const order = orders.find(o => o.order_id === orderId);
            if (!order) return;

            await api.upsert('orders', {
                id: order.id,
                order_id: orderId,
                status: nextStatus as any,
                updated_at: new Date().toISOString().slice(0, 19).replace('T', ' ')
            });
            setOrders(prev => prev.map(o =>
                o.order_id === orderId ? { ...o, status: nextStatus as any } : o
            ));
        } catch (error) {
            console.error("Error updating order:", error);
            alert("No se pudo actualizar el estado. Inténtalo de nuevo.");
        }
    };

    const getStatusColor = (status: string) => {
        switch (status) {
            case 'PAID': return 'text-yellow-400 border-yellow-400';
            case 'PREPARING': return 'text-orange-400 border-orange-400';
            case 'READY': return 'text-green-400 border-green-400';
            case 'DELIVERED': return 'text-gray-400 border-gray-400';
            default: return 'text-white border-white';
        }
    };

    const displayedOrders = orders.filter(o =>
        showHistory ? o.status === 'DELIVERED' : o.status !== 'DELIVERED'
    );

    return (
        <Layout>
            <div className="text-white">
                <div className="flex justify-between items-center mb-8">
                    <h1 className="text-3xl font-black italic tracking-wider flex items-center gap-3">
                        <ShoppingBag className="text-red-500" />
                        {showHistory ? "HISTORIAL DE PEDIDOS" : "GESTIÓN DE PEDIDOS"}
                    </h1>
                    <button
                        onClick={() => setShowHistory(!showHistory)}
                        className={`flex items-center gap-2 px-4 py-2 rounded-xl font-bold transition-all ${showHistory ? 'bg-red-600 hover:bg-red-500' : 'bg-gray-800 hover:bg-gray-700'}`}
                    >
                        <Clock size={18} />
                        {showHistory ? "VER ACTIVOS" : "VER HISTORIAL"}
                    </button>
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                    {displayedOrders.map((order) => {
                        let items: OrderLine[] = [];
                        try {
                            const parsed = JSON.parse(order.items_json);
                            items = Array.isArray(parsed) ? parsed : [];
                        } catch (e) {
                            items = [];
                        }

                        return (
                            <div key={order.order_id} className={`bg-[#171717] rounded-2xl p-6 border-l-4 ${order.status === 'READY' ? 'border-green-500' : (order.status === 'PREPARING' ? 'border-orange-500' : (order.status === 'DELIVERED' ? 'border-gray-600' : 'border-yellow-500'))} shadow-lg`}>
                                <div className="flex justify-between items-start mb-4">
                                    <div>
                                        <h3 className="text-xl font-bold">#{order.order_id.slice(-8).toUpperCase()}</h3>
                                        <p className="text-gray-400 text-sm">{order.created_at}</p>
                                    </div>
                                    <div className="flex items-center gap-3">
                                        <div className={`px-3 py-1 rounded-full text-xs font-bold ${getStatusColor(order.status)} border border-current`}>
                                            {order.status === 'PAID' ? 'RECIBIDO' : (order.status === 'PREPARING' ? 'COCINANDO' : (order.status === 'DELIVERED' ? 'ENTREGADO' : order.status))}
                                        </div>
                                        <button
                                            onClick={() => deleteOrder(order.id)}
                                            className="text-gray-600 hover:text-red-500 transition-colors p-1"
                                            title="Eliminar Pedido"
                                        >
                                            <Trash2 size={18} />
                                        </button>
                                    </div>
                                </div>

                                <div className="space-y-2 mb-6">
                                    {items.map((item, idx) => {
                                        const pid = item.product_id || item.id || "";
                                        const catalogItem = productsMap[pid];
                                        const name = item.name || catalogItem?.name || "Producto Desconocido";
                                        const price = item.unit_price ?? item.price ?? Number(catalogItem?.price) ?? 0;
                                        const quantity = item.quantity ?? 1;

                                        return (
                                            <div key={idx} className="flex justify-between border-b border-gray-800 pb-2">
                                                <span>
                                                    <span className="font-bold text-gray-400 mr-2">{quantity}x</span>
                                                    {name}
                                                </span>
                                                <span className="font-mono">€{(price * quantity).toFixed(2)}</span>
                                            </div>
                                        );
                                    })}
                                </div>

                                <div className="flex justify-between items-center mt-4">
                                    <div className="text-2xl font-black">€{(order.total_amount || 0).toFixed(2)}</div>

                                    {order.status === 'PAID' && (
                                        <button
                                            onClick={() => toggleStatus(order.order_id, order.status)}
                                            className="flex items-center gap-2 bg-blue-600 hover:bg-blue-500 text-white px-6 py-3 rounded-xl font-bold transition-colors"
                                        >
                                            <ChefHat size={20} />
                                            A COCINA
                                        </button>
                                    )}

                                    {order.status === 'PREPARING' && (
                                        <button
                                            onClick={() => toggleStatus(order.order_id, order.status)}
                                            className="flex items-center gap-2 bg-orange-600 hover:bg-orange-500 text-white px-6 py-3 rounded-xl font-bold transition-colors"
                                        >
                                            <Flame size={20} />
                                            TERMINAR
                                        </button>
                                    )}

                                    {order.status === 'READY' && (
                                        <button
                                            onClick={() => toggleStatus(order.order_id, order.status)}
                                            className="flex items-center gap-2 bg-green-600 hover:bg-green-500 text-white px-6 py-3 rounded-xl font-bold transition-colors"
                                        >
                                            <CheckCircle size={20} />
                                            ENTREGADO
                                        </button>
                                    )}

                                    {order.status === 'DELIVERED' && (
                                        <span className="text-gray-500 font-bold italic">COMPLETADO</span>
                                    )}
                                </div>
                            </div>
                        );
                    })}
                </div>

                {displayedOrders.length === 0 && !loading && (
                    <div className="text-center text-gray-500 mt-20">
                        <Clock size={48} className="mx-auto mb-4" />
                        <p>{showHistory ? "No hay pedidos en el historial" : "No hay pedidos activos"}</p>
                    </div>
                )}
            </div>
        </Layout>
    );
};

export default OrdersPage;
