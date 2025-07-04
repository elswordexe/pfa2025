import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import { Login } from "./pages/Login";
import DashboardAdminClient from "./pages/DashboardAdminClient";
import UserManagement from "./pages/UserManagement";
import "./App.css"; 
import InventairePlan from "./pages/InventairePlan";
import Inventorytest from "./pages/Inventory";
import VerifyAccount from "./pages/VerifyAccount";
import ResetPassword from './components/ResetPassword';
import AjouterProduit from './pages/AjouterProduit';
import ListeProduits from './pages/ListeProduits';
import Analytics from './pages/Analytics';
import AuthForm from './components/AuthForm';
import ZoneManagement from './pages/ZoneManagement';
import PlanManagement from './pages/PlanManagement';
import Dashboardsuperadmin from './pages/DashboardSuperAdmin';
import { useEffect } from 'react';
import { getRoleFromToken, isTokenExpired } from './utils/auth';

function Logout() {
  useEffect(() => {
    localStorage.clear();
    window.location.href = '/login';
  }, []);
  return null;
}


function App() {
  const role = getRoleFromToken();
  const isAuthenticated = !!localStorage.getItem('token') && !isTokenExpired();

  useEffect(() => {
    const checkToken = () => {
      if (isTokenExpired()) {
        localStorage.removeItem('token');
        const publicPaths = ['/login', '/register', '/reset-password', '/verify'];
        if (!publicPaths.includes(window.location.pathname)) {
          window.location.href = '/login';
        }
      }
    };
    checkToken();
    const intervalId = setInterval(checkToken, 60000);
    return () => clearInterval(intervalId);
  }, []);

  return (
    <Router>
      <Routes>
        <Route path="/login" element={<AuthForm mode="login" />} />
        <Route path="/register" element={<AuthForm mode="register" />} />
        <Route path="/verify" element={<VerifyAccount />} />
        <Route path="/reset-password" element={<ResetPassword />} />
        {isAuthenticated ? (
          <>
            <Route path="/dashsuperadmin" element={<Dashboardsuperadmin />} />
            <Route path="/dashboard" element={<DashboardAdminClient />} />
            <Route path="/users" element={<UserManagement />} />
            <Route path="/plans" element={role === 'SUPER_ADMIN' ? <PlanManagement /> : <InventairePlan />} />
            <Route path="/inventory" element={<Inventorytest />} />
            <Route path="/produits/ajouter" element={<AjouterProduit />} />
            <Route path="/produits" element={<ListeProduits />} />
            <Route path="/analytics" element={<Analytics />} />
            <Route path="/zones" element={<ZoneManagement />} />
            <Route path="/plans-management" element={<PlanManagement />} />
            <Route path="/logout" element={<Logout />} />
            <Route path="/" element={<DashboardAdminClient />} />
          </>
        ) : (
          <Route path="*" element={<AuthForm mode="login" />} />
        )}
      </Routes>
    </Router>
  );
}

export default App;
