import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { ToastProvider } from "./context/ToastContext";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { Login } from "./pages/Login";
import { ZonesMap } from "./pages/ZonesMap";
import { Routes as RoutesPage } from "./pages/Routes";
import { Dashboard } from "./pages/Dashboard";
import { Beacons } from "./pages/Beacons";
import { Statistics } from "./pages/Statistics";
import { BeaconDetail } from "./pages/BeaconDetail";
import { Emergencies } from "./pages/Emergencies";
import { Incidents } from "./pages/Incidents";
import { CircuitState } from "./pages/CircuitState";
import { Logs } from "./pages/Logs";
import { Config } from "./pages/Config";
import OrdersPage from "./pages/OrdersPage";
import ProductsPage from "./pages/ProductsPage";
import FoodStandsPage from "./pages/FoodStandsPage";
import NewsPage from "./pages/NewsPage";
import UsersPage from "./pages/UsersPage";
import "./index.css";

function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute>
                  <Dashboard />
                </ProtectedRoute>
              }
            />
            <Route
              path="/users"
              element={
                <ProtectedRoute>
                  <UsersPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/beacons"
              element={
                <ProtectedRoute>
                  <Beacons />
                </ProtectedRoute>
              }
            />
            <Route
              path="/beacons/:beaconId"
              element={
                <ProtectedRoute>
                  <BeaconDetail />
                </ProtectedRoute>
              }
            />
            <Route
              path="/incidents"
              element={
                <ProtectedRoute>
                  <Incidents />
                </ProtectedRoute>
              }
            />
            <Route
              path="/circuit-state"
              element={
                <ProtectedRoute>
                  <CircuitState />
                </ProtectedRoute>
              }
            />
            <Route
              path="/logs"
              element={
                <ProtectedRoute>
                  <Logs />
                </ProtectedRoute>
              }
            />
            <Route
              path="/zones"
              element={
                <ProtectedRoute>
                  <ZonesMap />
                </ProtectedRoute>
              }
            />
            <Route
              path="/routes"
              element={
                <ProtectedRoute>
                  <RoutesPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/statistics"
              element={
                <ProtectedRoute>
                  <Statistics />
                </ProtectedRoute>
              }
            />
            <Route
              path="/emergencies"
              element={
                <ProtectedRoute>
                  <Emergencies />
                </ProtectedRoute>
              }
            />
            <Route
              path="/config"
              element={
                <ProtectedRoute>
                  <Config />
                </ProtectedRoute>
              }
            />
            <Route
              path="/orders"
              element={
                <ProtectedRoute>
                  <OrdersPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/products"
              element={
                <ProtectedRoute>
                  <ProductsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/food-stands"
              element={
                <ProtectedRoute>
                  <FoodStandsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/news"
              element={
                <ProtectedRoute>
                  <NewsPage />
                </ProtectedRoute>
              }
            />
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </BrowserRouter>
      </ToastProvider>
    </AuthProvider>
  );
}

export default App;
