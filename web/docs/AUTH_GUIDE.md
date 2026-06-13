# Autenticación del Panel de Control

El panel de control ya tiene implementado Firebase Authentication con email y contraseña.

## 🔐 Inicio de Sesión

### Página de Login
- Ruta: `/login`
- Redirección automática si no estás autenticado
- Formulario con email y contraseña

### Usuarios Existentes

Actualmente hay **1 usuario** registrado. Para ver sus datos:

```bash
firebase auth:export users.json
```

## 👤 Crear Usuario Administrador

### Opción 1: Desde Firebase Console (Recomendado)

1. Ir a [Firebase Console](https://console.firebase.google.com/project/panel-de-control-georacing/authentication/users)
2. Click en "Add user"
3. Ingresar:
   - Email: `admin@georacing.com`
   - Password: (mínimo 6 caracteres)
4. Click en "Add user"

### Opción 2: Desde Script

```bash
npm install -D tsx
npm run create-admin
```

Esto creará un usuario con:
- **Email:** `admin@georacing.com`
- **Contraseña:** `Admin123456`

### Opción 3: Mediante Firebase CLI

```bash
firebase auth:import users.json
```

Con archivo `users.json`:
```json
{
  "users": [
    {
      "localId": "unique-id",
      "email": "admin@georacing.com",
      "passwordHash": "hash",
      "salt": "salt"
    }
  ]
}
```

## 🚀 Uso del Panel

1. **Iniciar aplicación:**
   ```bash
   npm run dev
   ```

2. **Acceder:** http://localhost:3000

3. **Login:**
   - Será redirigido automáticamente a `/login`
   - Ingresar email y contraseña
   - Click en "Iniciar sesión"

4. **Logout:**
   - Click en "Cerrar sesión" en la esquina superior derecha

## 🔒 Seguridad

### Rutas Protegidas

Todas las rutas están protegidas con `ProtectedRoute`:
- `/dashboard` - Listado de beacons
- `/beacons/:id` - Detalle de beacon
- `/emergencies` - Control de emergencias

Si no estás autenticado, serás redirigido a `/login`.

### Context de Autenticación

El `AuthContext` provee:
```typescript
{
  user: User | null,           // Usuario actual
  loading: boolean,            // Cargando estado
  login: (email, password),    // Función de login
  logout: ()                   // Función de logout
}
```

## 🔧 Configuración de Firebase Auth

### Métodos Habilitados

- ✅ Email/Password

### Para Habilitar Otros Métodos

1. Ir a Firebase Console → Authentication → Sign-in method
2. Habilitar métodos adicionales:
   - Google
   - Microsoft
   - GitHub
   - etc.

## 📱 Cambiar Contraseña

### Desde Firebase Console

1. Authentication → Users
2. Click en el usuario
3. "Reset password"

### Programáticamente

Agregar en la página de perfil:

```typescript
import { updatePassword, sendPasswordResetEmail } from "firebase/auth";

// Cambiar contraseña
await updatePassword(user, newPassword);

// Enviar email de reset
await sendPasswordResetEmail(auth, email);
```

## 🛡️ Reglas de Seguridad

Las reglas de Firestore ya están configuradas para requerir autenticación:
- Solo usuarios autenticados pueden crear/modificar beacons
- Solo usuarios autenticados pueden crear logs de emergencia
- Las beacons pueden auto-registrarse y actualizar heartbeat

## 🔄 Recuperar Contraseña (Futuro)

Para agregar recuperación de contraseña en `/login`:

```typescript
const handleForgotPassword = async () => {
  try {
    await sendPasswordResetEmail(auth, email);
    alert("Email de recuperación enviado");
  } catch (error) {
    console.error(error);
  }
};
```

## ✅ Estado Actual

- ✅ Firebase Authentication habilitado
- ✅ Login con email/password funcional
- ✅ Rutas protegidas configuradas
- ✅ Logout implementado
- ✅ Redirección automática
- ✅ Context de autenticación global
