import { Layout } from "../components/Layout";
import { Settings, User, Bell, Shield, Database } from "lucide-react";

export function Config() {
  return (
    <Layout>
      <div className="space-y-6">
        <div className="bg-dark-800 border border-dark-700 rounded-lg p-6">
          <div className="flex items-center gap-3 mb-6">
            <Settings className="w-6 h-6 text-blue-400" />
            <h2 className="text-2xl font-semibold text-white">Configuración del Sistema</h2>
          </div>

          <div className="space-y-4">
            {/* Cuenta */}
            <div className="p-4 bg-dark-700 rounded-lg border border-dark-600">
              <div className="flex items-center gap-3 mb-2">
                <User className="w-5 h-5 text-gray-400" />
                <h3 className="text-lg font-medium text-white">Cuenta de Usuario</h3>
              </div>
              <p className="text-sm text-gray-400 ml-8">
                Gestiona tu perfil y preferencias de usuario
              </p>
            </div>

            {/* Notificaciones */}
            <div className="p-4 bg-dark-700 rounded-lg border border-dark-600">
              <div className="flex items-center gap-3 mb-2">
                <Bell className="w-5 h-5 text-gray-400" />
                <h3 className="text-lg font-medium text-white">Notificaciones</h3>
              </div>
              <p className="text-sm text-gray-400 ml-8">
                Configura alertas y notificaciones del sistema
              </p>
            </div>

            {/* Seguridad */}
            <div className="p-4 bg-dark-700 rounded-lg border border-dark-600">
              <div className="flex items-center gap-3 mb-2">
                <Shield className="w-5 h-5 text-gray-400" />
                <h3 className="text-lg font-medium text-white">Seguridad</h3>
              </div>
              <p className="text-sm text-gray-400 ml-8">
                Administra permisos y accesos del sistema
              </p>
            </div>

            {/* Base de Datos */}
            <div className="p-4 bg-dark-700 rounded-lg border border-dark-600">
              <div className="flex items-center gap-3 mb-2">
                <Database className="w-5 h-5 text-gray-400" />
                <h3 className="text-lg font-medium text-white">Base de Datos</h3>
              </div>
              <p className="text-sm text-gray-400 ml-8">
                Configuración de Firebase y almacenamiento
              </p>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
}
