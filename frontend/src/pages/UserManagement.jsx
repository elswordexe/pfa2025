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
import Modal from '@mui/joy/Modal';
import ModalDialog from '@mui/joy/ModalDialog';
import FormControl from '@mui/joy/FormControl';
import FormLabel from '@mui/joy/FormLabel';
import Input from '@mui/joy/Input';
import Select from '@mui/joy/Select';
import Option from '@mui/joy/Option';
import Stack from '@mui/joy/Stack';

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
      const updatedUser = { ...editingUser, ...formData };
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
    try {
      const newUser = { ...formData };
      await axios.post('http://localhost:8080/users/register', newUser, { headers: authHeaders });
      setAddModalOpen(false);
      setFormData({ nom:'', prenom:'', email:'', role:'Utilisateur' });
      fetchUsers();
    } catch (error) {
      console.error('Erreur création', error);
      alert('Erreur lors de la création');
    }
  };

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', backgroundColor: '#ffffff' }}>
      <Sidebarsuper />
      <Box sx={{ flex: 1, p: 4 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
          <Typography level="h2" sx={{ color: '#0d0202' }}>Gestion des utilisateurs</Typography>
          <Button startDecorator={<AddIcon />} variant="solid" color="dark" onClick={() => setAddModalOpen(true)}>
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

        <Modal open={addModalOpen} onClose={() => setAddModalOpen(false)}>
          <ModalDialog sx={{ minWidth: 400 }}>
            <Typography level="h4" mb={2}>Nouvel utilisateur</Typography>
            <Stack spacing={2}>
              <FormControl>
                <FormLabel>Nom</FormLabel>
                <Input value={formData.nom} onChange={e=>setFormData({...formData, nom:e.target.value})} />
              </FormControl>
              <FormControl>
                <FormLabel>Prénom</FormLabel>
                <Input value={formData.prenom} onChange={e=>setFormData({...formData, prenom:e.target.value})} />
              </FormControl>
              <FormControl>
                <FormLabel>Email</FormLabel>
                <Input value={formData.email} onChange={e=>setFormData({...formData, email:e.target.value})} />
              </FormControl>
              <FormControl>
                <FormLabel>Rôle</FormLabel>
                <Select value={formData.role} onChange={(_,v)=>setFormData({...formData, role:v})}>
                  {roles.map(r=> <Option value={r} key={r}>{r}</Option>)}
                </Select>
              </FormControl>
              <Stack direction="row" spacing={1} justifyContent="flex-end">
                <Button variant="plain" onClick={()=>setAddModalOpen(false)}>Annuler</Button>
                <Button variant="solid" color="success" onClick={handleAddUser}>Créer</Button>
              </Stack>
            </Stack>
          </ModalDialog>
        </Modal>
      </Box>
    </Box>
  );
}
