import DriverDashboard from "./pages/DriverDashboard";
import Login from "./pages/Login";
import PassengerDashboard from "./pages/PassengerDashboard";
import { useAuthStore } from "./store/useAuthStore";
import { BrowserRouter, Navigate, Route, Router, Routes } from "react-router-dom";





// A simple wrapper to protect routes
const ProtectedRoute = ({ children, allowedRole }: { children: JSX.Element, allowedRole: string }) => {
  const { token, role } = useAuthStore();
  if (!token) return <Navigate to="/login" />;
  if (role !== allowedRole) return <Navigate to="/unauthorized" />;
  return children;
};

export default function App() {
  return (
      <BrowserRouter>
          <Routes>
              <Route path="/login" element={<Login />} />
              
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
