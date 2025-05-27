import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import { Login } from "./pages/Login";
import { DashboardSuperAdmin} from "./pages/DashboardSuperAdmin";
import Dash from "./components/Dash";

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/dashboard" element={<Dash />} />
      </Routes>
    </Router>
  );
}

export default App;
