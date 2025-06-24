import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import { Login } from "./pages/Login";
import Dash from "./components/Dash";
import UserManagement from "./pages/UserManagement";
import "./app.css"; 
import InventairePlan from "./pages/InventairePlan";
import Inventorytest from "./pages/Inventory";
import VerifyAccount from "./pages/VerifyAccount";
import ResetPassword from './components/ResetPassword';
import AjouterProduit from './pages/AjouterProduit';
import ListeProduits from './pages/ListeProduits';
import Analytics from './pages/Analytics';
import AuthForm from './components/AuthForm';
import ZoneManagement from './pages/ZoneManagement';
import ClientManagement from './pages/ClientManagement';
import PlanManagement from './pages/PlanManagement';
import Dashboardsuperadmin from './components/DashSuper';
import { useEffect } from 'react';
import { getRoleFromToken } from './utils/auth';

function Logout() {
  useEffect(() => {
    localStorage.clear();
    window.location.href = '/login';
  }, []);
  return null;
}

function App() {
  const role = getRoleFromToken();

  return (
    <Router>
      <Routes>
        <Route path="/dashsuperadmin" element={<Dashboardsuperadmin />} />
        <Route path="/" element={<Login />} />
        <Route path="/dashboard" element={<Dash />} />
        <Route path="/users" element={<UserManagement />} />
        <Route path="/plans" element={role === 'SUPER_ADMIN' ? <PlanManagement /> : <InventairePlan />} />
        <Route path="/inventory" element={<Inventorytest />} />
        <Route path="/verify" element={<VerifyAccount />} />
        <Route path="/reset-password" element={<ResetPassword />} />
        <Route path="/produits/ajouter" element={<AjouterProduit />} />
        <Route path="/produits" element={<ListeProduits />} />
        <Route path="/analytics" element={<Analytics />} />
        <Route path="/login" element={<AuthForm mode="login" />} />
        <Route path="/register" element={<AuthForm mode="register" />} />
        <Route path="/zones" element={<ZoneManagement />} />
        <Route path="/plans-management" element={<PlanManagement />} />
        <Route path="/logout" element={<Logout />} />
      </Routes>
    </Router>
  );
}

export default App;
