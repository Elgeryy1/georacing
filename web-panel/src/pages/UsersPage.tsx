import React, { useState, useEffect } from "react";
import { api } from "../services/apiClient";
import { Users, Award, Search, Loader2 } from "lucide-react";
import { useToast } from "../context/ToastContext";
import { Layout } from "../components/Layout";

interface UserDB {
  id: string;
  uid: string;
  email: string;
  display_name: string;
  photo_url?: string;
}

interface FanProfile {
  id: string; // from gamification_profile
  totalXP: number;
  level: number;
  circuitsVisited: number;
  kmWalked: number;
}

const COLLECTIBLES_MAP: Record<string, string> = {
  "c01": "Fernando Alonso", "c02": "Lewis Hamilton", "c03": "Max Verstappen", "c04": "Marc Márquez", "c05": "Pecco Bagnaia",
  "c06": "Marchador", "c07": "Corredor", "c25": "Maratonista",
  "c08": "Primera Foto", "c09": "Fotógrafo", "c10": "Paparazzi",
  "c11": "Primer Pedido", "c12": "Gourmet", "c13": "Master Chef",
  "c14": "VIP Access", "c15": "Pit Lane",
  "c16": "Eco Warrior", "c17": "Planeta Verde",
  "c18": "Nocturno", "c19": "Madrugador", "c20": "Bajo la Lluvia",
  "c21": "Leyenda GeoRacing", "c22": "Fiel al Circuito", "c23": "El Primero", "c24": "Grupo Legendario"
};

export const UsersPage: React.FC = () => {
  const [realUsers, setRealUsers] = useState<UserDB[]>([]);
  const [profiles, setProfiles] = useState<Record<string, FanProfile>>({});
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedUser, setSelectedUser] = useState<UserDB | null>(null);
  const [givingCollectible, setGivingCollectible] = useState<string | null>(null);
  const { showToast } = useToast();

  const fetchUsers = async () => {
    try {
      setLoading(true);
      // Fetch both real users and the gamification profiles
      const usersRes = await api.get<UserDB>("users") || [];
      const profilesRes = await api.get<FanProfile>("gamification_profile") || [];

      // Usualmente mapearíamos por el uid real. Pero como la app asume current_user a veces, mapearemos
      // tanto el current_user como cualquier profile asociado por id.
      const profileMap: Record<string, FanProfile> = {};
      profilesRes.forEach(p => {
        profileMap[p.id] = p; // id podría ser current_user, o el uid de firebase.
      });
      setProfiles(profileMap);

      // Desduplicar usuarios de la tabla DB (en caso de que la API devuelva múltiples filas por el mismo UID)
      const uniqueUsersMap = new Map<string, UserDB>();
      usersRes.forEach(u => {
        if (!uniqueUsersMap.has(u.uid)) uniqueUsersMap.set(u.uid, u);
      });
      const deduplicatedUsers = Array.from(uniqueUsersMap.values());

      const mergedUsers: UserDB[] = [...deduplicatedUsers];

      // Añadir perfiles que no estén en la tabla de usuarios
      profilesRes.forEach(p => {
        // Ignorar el perfil genérico "current_user" que se crea cuando la app se usa sin loguearse
        if (p.id.toLowerCase() === "current_user") return;

        const existsInUsers = usersRes.find(u => u.uid === p.id || u.id === p.id);
        if (!existsInUsers) {
          mergedUsers.push({
            id: p.id,
            uid: p.id,
            email: "Simulado en App (gamification_profile)",
            display_name: p.id
          });
        }
      });

      setRealUsers(mergedUsers);

    } catch (error) {
      console.error("Error fetching users:", error);
      showToast("Error al cargar usuarios", "error");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleGiveCollectible = async (uid: string, collectibleId: string) => {
    try {
      setGivingCollectible(collectibleId);

      // Upsert a user_collectibles para desbloquearlo permanentemente
      await api.upsert("user_collectibles", {
        id: `${uid}_${collectibleId}`,
        user_id: uid,
        collectible_id: collectibleId,
        unlocked: true,
        unlocked_at: Math.floor(Date.now() / 1000)
      });

      showToast(`Cromo ${COLLECTIBLES_MAP[collectibleId] || collectibleId} otorgado a ${uid}`, "success");
    } catch (error) {
      console.error("Error giving collectible:", error);
      showToast("Error al otorgar cromo", "error");
    } finally {
      setGivingCollectible(null);
    }
  };

  const filteredUsers = realUsers.filter((u) =>
    (u.display_name?.toLowerCase() || "").includes(searchTerm.toLowerCase()) ||
    (u.email?.toLowerCase() || "").includes(searchTerm.toLowerCase())
  );

  return (
    <Layout>
      <div className="space-y-6">
        <div className="flex justify-between items-center">
          <div>
            <h2 className="text-2xl font-bold flex items-center gap-2">
              <Users className="text-blue-500" />
              Usuarios Registrados
            </h2>
            <p className="text-gray-400">Ver fans de la app y otorgar cromos especiales</p>
          </div>
          <button
            onClick={fetchUsers}
            className="px-4 py-2 bg-dark-700 hover:bg-dark-600 rounded-lg flex items-center gap-2 text-sm"
          >
            <Loader2 className={`w-4 h-4 ${loading ? "animate-spin" : ""}`} />
            Refrescar
          </button>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Lista de Usuarios (Con Tabla Real) */}
          <div className="lg:col-span-1 space-y-4">
            <div className="relative">
              <Search className="w-5 h-5 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                placeholder="Buscar por Nombre / Email..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full pl-10 pr-4 py-2 bg-dark-800 border border-dark-700 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>

            <div className="bg-dark-800 rounded-xl overflow-hidden border border-dark-700">
              {loading && realUsers.length === 0 ? (
                <div className="p-8 text-center text-gray-400">Cargando usuarios reales...</div>
              ) : filteredUsers.length === 0 ? (
                <div className="p-8 text-center text-gray-400">No hay usuarios con este criterio</div>
              ) : (
                <div className="divide-y divide-dark-700 max-h-[600px] overflow-y-auto">
                  {filteredUsers.map((user) => {
                    // Mapeo solo al perfil real del usuario, evitando heredar el genérico "current_user"
                    const profile = profiles[user.uid] || null;
                    return (
                      <button
                        key={user.id}
                        onClick={() => setSelectedUser(user)}
                        className={`w-full text-left p-4 transition-all flex items-center gap-4 border-l-4 ${selectedUser?.id === user.id
                          ? "bg-dark-700/80 border-blue-500 shadow-md"
                          : "bg-transparent border-transparent hover:bg-dark-700/50 hover:border-dark-600"
                          }`}
                      >
                        {user.photo_url ? (
                          <img src={user.photo_url} alt="Profile" className="w-12 h-12 rounded-full bg-dark-900 border-2 border-dark-700 object-cover shadow-sm" />
                        ) : (
                          <div className="w-12 h-12 rounded-full bg-dark-900 border-2 border-dark-700 flex items-center justify-center shadow-sm">
                            <Users className="w-5 h-5 text-gray-500" />
                          </div>
                        )}

                        <div className="flex-1">
                          <div className="font-semibold text-sm line-clamp-1">{user.display_name || "Sin nombre"}</div>
                          <div className="text-xs text-gray-400 line-clamp-1">{user.email}</div>
                          {profile && (
                            <div className="flex items-center gap-3 mt-1 text-[10px] text-gray-500 font-medium">
                              <span className="flex items-center gap-1">
                                <Award className="w-3 h-3 text-purple-400" />
                                Nvl {Math.floor((profile.totalXP || 0) / 250) + 1}
                              </span>
                              <span>{profile.totalXP || 0} XP</span>
                            </div>
                          )}
                        </div>
                      </button>
                    );
                  })}
                </div>
              )}
            </div>
          </div>

          {/* Detalles y Cromos */}
          <div className="lg:col-span-2">
            {selectedUser ? (
              <div className="space-y-6">
                {/* Info de Perfil */}
                <div className="bg-dark-800 rounded-xl p-6 border border-dark-700 flex flex-col sm:flex-row items-center sm:items-start gap-6 shadow-sm">
                  {selectedUser.photo_url ? (
                    <img src={selectedUser.photo_url} alt="Profile" className="w-24 h-24 rounded-full bg-dark-900 shadow-xl border-4 border-dark-700 object-cover" />
                  ) : (
                    <div className="w-24 h-24 rounded-full bg-dark-900 border-4 border-dark-700 flex items-center justify-center shadow-xl">
                      <Users className="w-10 h-10 text-gray-600" />
                    </div>
                  )}
                  <div className="flex-1 text-center sm:text-left mt-2 sm:mt-0">
                    <h3 className="text-3xl font-bold text-white mb-1">
                      {selectedUser.display_name || "Sin nombre"}
                    </h3>
                    <div className="text-blue-400 font-medium mb-3">{selectedUser.email || "Sin email"}</div>
                    <div className="inline-block bg-dark-900 px-3 py-1.5 rounded-lg border border-dark-700">
                      <p className="text-xs text-gray-400 uppercase font-mono tracking-wider">
                        UID: <span className="text-gray-200">{selectedUser.uid}</span>
                      </p>
                    </div>
                  </div>
                </div>

                {/* Estadísticas del usuario */}
                {(() => {
                  const profile = profiles[selectedUser.uid];
                  if (!profile) return (
                    <div className="bg-dark-800 rounded-xl p-6 border border-dark-700 text-center text-gray-500 shadow-sm text-sm">
                      No hay datos de gamificación disponibles para este usuario.
                    </div>
                  );
                  return (
                    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                      <div className="bg-dark-800 border border-dark-700 p-5 rounded-xl text-center shadow-sm">
                        <div className="text-xs text-gray-400 uppercase tracking-widest mb-1 font-semibold">Nivel</div>
                        <div className="text-3xl font-extrabold text-blue-500">
                          {Math.floor((profile.totalXP || 0) / 250) + 1}
                        </div>
                      </div>
                      <div className="bg-dark-800 border border-dark-700 p-5 rounded-xl text-center shadow-sm">
                        <div className="text-xs text-gray-400 uppercase tracking-widest mb-1 font-semibold">Total XP</div>
                        <div className="text-3xl font-extrabold text-white">{profile.totalXP || 0}</div>
                      </div>
                      <div className="bg-dark-800 border border-dark-700 p-5 rounded-xl text-center shadow-sm">
                        <div className="text-xs text-gray-400 uppercase tracking-widest mb-1 font-semibold">Km Recorridos</div>
                        <div className="text-3xl font-extrabold text-green-400">{profile.kmWalked || 0}</div>
                      </div>
                      <div className="bg-dark-800 border border-dark-700 p-5 rounded-xl text-center shadow-sm">
                        <div className="text-xs text-gray-400 uppercase tracking-widest mb-1 font-semibold">Circuitos</div>
                        <div className="text-3xl font-extrabold text-yellow-500">{profile.circuitsVisited || 0}</div>
                      </div>
                    </div>
                  );
                })()}

                {/* Botonera de Cromos */}
                <div className="bg-dark-800 rounded-xl overflow-hidden border border-dark-700 shadow-sm">
                  <div className="p-5 border-b border-dark-700 bg-dark-800/50">
                    <h3 className="text-lg font-bold flex items-center gap-2 text-white">
                      <Award className="w-5 h-5 text-yellow-500" />
                      Otorgar Cromos Digitales
                    </h3>
                    <p className="text-xs text-gray-400 mt-1">Haz clic en un cromo para adjuntarlo directamente a la cuenta del usuario seleccionado.</p>
                  </div>

                  <div className="p-5 grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-5 gap-4">
                    {Object.entries(COLLECTIBLES_MAP).map(([id, name]) => (
                      <button
                        key={id}
                        onClick={() => handleGiveCollectible(selectedUser.uid, id)}
                        disabled={givingCollectible === id}
                        className="group flex flex-col items-center justify-center p-4 rounded-xl border border-dark-700 bg-dark-900/40 hover:bg-dark-700 hover:border-yellow-500/40 transition-all disabled:opacity-50"
                      >
                        {givingCollectible === id ? (
                          <Loader2 className="w-10 h-10 animate-spin text-yellow-500 mb-3" />
                        ) : (
                          <div className="relative w-12 h-16 bg-gradient-to-br from-yellow-800/80 to-yellow-900/80 rounded-sm mb-3 flex items-center justify-center shadow-md border border-yellow-500/20 group-hover:from-yellow-600 group-hover:to-orange-500 transition-all duration-300 transform group-hover:-translate-y-1">
                            <div className="absolute inset-0 bg-gradient-to-tr from-white/0 via-white/10 to-white/0 opacity-0 group-hover:opacity-100 transition-opacity"></div>
                            <span className="text-sm font-bold text-yellow-200/80 group-hover:text-white drop-shadow-sm">{id}</span>
                          </div>
                        )}
                        <span className="text-[11px] leading-tight text-center font-medium text-gray-400 group-hover:text-white transition-colors">{name}</span>
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            ) : (
              <div className="h-full bg-dark-800 rounded-xl border border-dark-700 flex flex-col items-center justify-center p-12 text-gray-500 min-h-[500px] shadow-sm">
                <Users className="w-16 h-16 mb-4 opacity-50 text-gray-600" />
                <p className="text-lg font-medium text-gray-400">Selecciona un usuario</p>
                <p className="text-sm mt-2 opacity-70">Haz clic en un fan de la lista izquierda para ver sus estadísticas o regalarle cromos.</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
};

export default UsersPage;
