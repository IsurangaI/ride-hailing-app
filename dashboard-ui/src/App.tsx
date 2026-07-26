import DriverDashboard from "./pages/DriverDashboard";
import Login from "./pages/Login";
import Register from "./pages/Register";
import PassengerDashboard from "./pages/PassengerDashboard";
import { useAuthStore } from "./store/useAuthStore";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";

const ProtectedRoute = ({ children, allowedRole }: { children: React.ReactNode, allowedRole: string }) => {
  const { token, role } = useAuthStore();
  if (!token) return <Navigate to="/login" />;
  if (role !== allowedRole) return <Navigate to="/unauthorized" />;
  return children;
};

export default function App() {
  return (
      <BrowserRouter>
          <Routes>
              <Route path="/" element={<Navigate to="/login" />} />
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />

              <Route path="/rider" element={
                  <ProtectedRoute allowedRole="PASSENGER">
                      <PassengerDashboard />
                  </ProtectedRoute>
              } />

              <Route path="/driver" element={
                  <ProtectedRoute allowedRole="DRIVER">
                      <DriverDashboard />
                  </ProtectedRoute>
              } />
          </Routes>
      </BrowserRouter>
  );
}
