import React, { useEffect, useState } from 'react';
import axios from 'axios';
import Box from '@mui/joy/Box';
import Typography from '@mui/joy/Typography';
import Button from '@mui/joy/Button';
import Table from '@mui/joy/Table';
import IconButton from '@mui/joy/IconButton';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import Sidebarsuper from '../components/Sidebar';
const roles = ["SUPER_ADMIN", "ADMIN_CLIENT", "AGENT_INVENTAIRE", "Utilisateur"];

export default function UserManagement() {
  const [users, setUsers] = useState([]);
  const [editingUser, setEditingUser] = useState(null);
  const [formData, setFormData] = useState({
    nom: '',
    prenom: '',
    email: '',
    role: 'Utilisateur',
  });

  const fetchUsers = async () => {
    try {
      const response = await axios.get('http://localhost:8080/users');
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
      const updatedUser = { ...editingUser, ...formData };
      await axios.put(`http://localhost:8080/users/${editingUser.id}`, updatedUser);
      setEditingUser(null);
      fetchUsers();
    } catch (error) {
      console.error('Erreur lors de la mise à jour', error);
      alert("Erreur lors de la mise à jour utilisateur");
    }
  };

  const handleDelete = (userId) => {
    alert("Suppression non encore implémentée côté backend.");
  };

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', backgroundColor: '#fff5f5' }}>
      <Sidebarsuper />
      <Box sx={{ flex: 1, p: 4 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
          <Typography level="h2" sx={{ color: '#b71c1c' }}>Gestion des utilisateurs</Typography>
          <Button startDecorator={<AddIcon />} variant="solid" color="danger">
            Ajouter
          </Button>
        </Box>

        <Table variant="outlined" borderAxis="both" hoverRow>
          <thead>
            <tr style={{ backgroundColor: '#ffebee' }}>
              <th>Nom</th>
              <th>Prénom</th>
              <th>Email</th>
              <th>Rôle</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id}>
                <td>
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
                <td>
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
                <td>
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
                <td>
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
                <td>
                  {editingUser?.id === user.id ? (
                    <>
                      <Button size="sm" color="success" onClick={handleSave}>Enregistrer</Button>
                      <Button size="sm" color="neutral" onClick={handleCancel}>Annuler</Button>
                    </>
                  ) : (
                    <>
                      <IconButton onClick={() => handleEdit(user)} color="primary"><EditIcon /></IconButton>
                      <IconButton color="danger" onClick={() => handleDelete(user.id)}><DeleteIcon /></IconButton>
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      </Box>
    </Box>
  );
}
