import React, { useEffect, useState } from 'react';
import axios from 'axios';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import Tooltip from '@mui/material/Tooltip';
import Sidebar from '../components/Sidebar';
import IconButton from '@mui/material/IconButton';
const roles = ["SUPER_ADMIN", "ADMIN_CLIENT", "AGENT_INVENTAIRE", "Utilisateur"];

export default function UserManagement() {
  const [users, setUsers] = useState([]);
  const [editingUser, setEditingUser] = useState(null);
  const [formData, setFormData] = useState({
    nom: '',
    prenom: '',
    email: '',
    password: '',
    role: 'Utilisateur',
  });
  const [showPassword, setShowPassword] = useState(false);
  const token = localStorage.getItem('token');
  const authHeaders = token ? { 'Authorization': `Bearer ${token}` } : {};
  const [addModalOpen, setAddModalOpen] = useState(false);

  const fetchUsers = async () => {
    try {
      const response = await axios.get('http://localhost:8080/users', { headers: authHeaders });
      setUsers(response.data);
    } catch (error) {
      console.error('Erreur lors du chargement des utilisateurs', error);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleEdit = (user) => {
    setEditingUser(user);
    setFormData({
      nom: user.nom || '',
      prenom: user.prenom || '',
      email: user.email || '',
      role: user.role || 'Utilisateur',
    });
  };

  const handleCancel = () => {
    setEditingUser(null);
    setFormData({
      nom: '',
      prenom: '',
      email: '',
      role: 'Utilisateur',
    });
  };

  const handleSave = async () => {
    try {
      const updatedUser = { ...editingUser, ...formData, dtype: formData.role };
      await axios.put(`http://localhost:8080/users/${editingUser.id}`, updatedUser, { headers: authHeaders });
      setEditingUser(null);
      fetchUsers();
    } catch (error) {
      console.error('Erreur lors de la mise à jour', error);
      alert("Erreur lors de la mise à jour utilisateur");
    }
  };

  const handleDelete = (userId) => {
    if (!window.confirm('Confirmer la suppression de cet utilisateur ?')) return;
    axios.delete(`http://localhost:8080/users/${userId}`, { headers: authHeaders })
      .then(() => fetchUsers())
      .catch(err => {
        console.error('Erreur suppression', err);
        alert('Erreur lors de la suppression');
      });
  };
  const handleAddUser = async () => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!formData.email || !formData.password) {
      alert('Email et mot de passe sont obligatoires');
      return;
    }
    if (!emailRegex.test(formData.email)) {
      alert('Veuillez saisir un email valide.');
      return;
    }
    try {
      let newUser = { ...formData, dtype: formData.role };
      await axios.post('http://localhost:8080/users/register', newUser, { headers: authHeaders });
      setAddModalOpen(false);
      setFormData({ nom:'', prenom:'', email:'', password:'', role:'Utilisateur' });
      fetchUsers();
    } catch (error) {
      console.error('Erreur création', error);
      alert('Erreur lors de la création');
    }
  };

  return (
    <div className="flex min-h-screen bg-gradient-to-br from-blue-50 to-indigo-50">
      <Sidebar />
      <div className="flex-1 min-w-0 overflow-x-hidden">
        <div className="p-4 md:p-6">
          <div className="bg-white rounded-2xl shadow-xl p-6 mb-6">
            <div className="flex flex-col md:flex-row justify-between items-center mb-6">
              <h1 className="text-2xl md:text-3xl font-bold text-gray-800 flex items-center">
                <svg className="w-8 h-8 mr-3 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M8 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z" />
                  <path d="M6 3a2 2 0 00-2 2v11a2 2 0 002 2h8a2 2 0 002-2V5a2 2 0 00-2-2 3 3 0 01-3 3H9a3 3 0 01-3-3z" />
                </svg>
                Gestion des utilisateurs
              </h1>
              <button
                onClick={() => setAddModalOpen(true)}
                className="flex items-center gap-2 bg-gradient-to-r from-blue-600 to-indigo-700 hover:from-blue-700 hover:to-indigo-800 text-white px-4 py-2 rounded-lg shadow-md transition mt-4 md:mt-0"
              >
                <AddIcon /> Ajouter
              </button>
            </div>
            <div className="overflow-hidden rounded-xl border border-gray-200 shadow-sm">
              <table className="w-full">
                <thead className="bg-gradient-to-r from-blue-600 to-indigo-700 text-white">
                  <tr>
                    <th className="p-4 text-left">Nom</th>
                    <th className="p-4 text-left">Prénom</th>
                    <th className="p-4 text-left">Email</th>
                    <th className="p-4 text-left">Rôle</th>
                    <th className="p-4 text-left">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((user) => (
                    <tr key={user.id} className="border-b border-gray-200 hover:bg-blue-50">
                      <td className="p-4 text-gray-900">
                        {editingUser?.id === user.id ? (
                          <input
                            value={formData.nom}
                            onChange={(e) => setFormData({ ...formData, nom: e.target.value })}
                            className="border rounded px-2 py-1"
                          />
                        ) : (
                          user.nom
                        )}
                      </td>
                      <td className="p-4 text-gray-900">
                        {editingUser?.id === user.id ? (
                          <input
                            value={formData.prenom}
                            onChange={(e) => setFormData({ ...formData, prenom: e.target.value })}
                            className="border rounded px-2 py-1"
                          />
                        ) : (
                          user.prenom
                        )}
                      </td>
                      <td className="p-4 text-gray-900">
                        {editingUser?.id === user.id ? (
                          <input
                            value={formData.email}
                            onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                            className="border rounded px-2 py-1"
                          />
                        ) : (
                          user.email
                        )}
                      </td>
                      <td className="p-4 text-gray-900">
                        {editingUser?.id === user.id ? (
                          <select
                            value={formData.role}
                            onChange={(e) => setFormData({ ...formData, role: e.target.value })}
                            className="border rounded px-2 py-1"
                          >
                            {roles.map((r) => (
                              <option key={r} value={r}>{r}</option>
                            ))}
                          </select>
                        ) : (
                          user.role
                        )}
                      </td>
                      <td className="p-4 flex gap-1">
                        {editingUser?.id === user.id ? (
                          <>
                            <button
                              className="px-3 py-1.5 bg-green-600 text-white rounded-lg shadow-md hover:bg-green-700 transition"
                              onClick={handleSave}
                            >Enregistrer</button>
                            <button
                              className="px-3 py-1.5 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-100 transition"
                              onClick={handleCancel}
                            >Annuler</button>
                          </>
                        ) : (
                          <>
                           <Tooltip title="Éditer" arrow>
        <IconButton
          style={{ color: '#FFD600' }}
          size="small"
          onClick={() => handleEdit(user)}
        >
          <EditIcon />
        </IconButton>
      </Tooltip>

                             <Tooltip title="Supprimer" arrow>
                           <IconButton
                               sx={{ color: 'error.main' }}
                              size="small"
                               onClick={() => handleDelete(user.id)}
                          >
                               <DeleteIcon />
                            </IconButton>
                            </Tooltip>

                          </>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
        {/* Modal for adding user */}
        {addModalOpen && (
          <div className="fixed inset-0 bg-white bg-opacity-50 flex items-center justify-center p-4 z-50">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
              <div className="bg-gradient-to-r from-blue-600 to-indigo-700 p-5">
                <h3 className="text-xl font-semibold text-white flex items-center">
                  <AddIcon className="mr-2" />
                  Nouvel utilisateur
                </h3>
              </div>
              <div className="p-6 space-y-4">
                <div>
                  <label className="block text-gray-700 mb-1">Nom</label>
                  <input
                    className="w-full p-2 border border-gray-300 rounded-lg"
                    value={formData.nom}
                    onChange={e => setFormData({ ...formData, nom: e.target.value })}
                  />
                </div>
                <div>
                  <label className="block text-gray-700 mb-1">Prénom</label>
                  <input
                    className="w-full p-2 border border-gray-300 rounded-lg"
                    value={formData.prenom}
                    onChange={e => setFormData({ ...formData, prenom: e.target.value })}
                  />
                </div>
                <div>
                  <label className="block text-gray-700 mb-1">Email</label>
                  <input
                    className="w-full p-2 border border-gray-300 rounded-lg"
                    value={formData.email}
                    onChange={e => setFormData({ ...formData, email: e.target.value })}
                    type="email"
                  />
                </div>
                <div>
                  <label className="block text-gray-700 mb-1">Mot de passe</label>
                  <div className="flex items-center">
                    <input
                      className="w-full p-2 border border-gray-300 rounded-lg"
                      value={formData.password}
                      onChange={e => setFormData({ ...formData, password: e.target.value })}
                      type={showPassword ? 'text' : 'password'}
                    />
                    <button
                      type="button"
                      className="ml-2 px-3 py-1.5 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-100 transition"
                      onClick={() => setShowPassword(v => !v)}
                    >
                      {showPassword ? 'Cacher' : 'Voir'}
                    </button>
                  </div>
                </div>
                <div>
                  <label className="block text-gray-700 mb-1">Rôle</label>
                  <select
                    className="w-full p-2 border border-gray-300 rounded-lg"
                    value={formData.role}
                    onChange={e => setFormData({ ...formData, role: e.target.value })}
                  >
                    {roles.map(r => (
                      <option key={r} value={r}>{r}</option>
                    ))}
                  </select>
                </div>
                <div className="flex justify-end gap-3">
                  <button
                    className="px-5 py-2.5 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-100 transition"
                    onClick={() => setAddModalOpen(false)}
                  >Annuler</button>
                  <button
                    className="px-5 py-2.5 bg-gradient-to-r from-blue-600 to-indigo-700 text-white rounded-lg hover:from-blue-700 hover:to-indigo-800 shadow-md transition"
                    onClick={handleAddUser}
                  >Créer</button>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
