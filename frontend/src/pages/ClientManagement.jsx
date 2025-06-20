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
import TextField from '@mui/joy/TextField';
import Modal from '@mui/joy/Modal';
import ModalDialog from '@mui/joy/ModalDialog';
import Sidebar from '../components/Sidebar';

export default function ClientManagement() {
  const [clients, setClients] = useState([]);
  const [editingClient, setEditingClient] = useState(null);
  const [formData, setFormData] = useState({
    nom: '',
    telephone: '',
    email: '',
  });
  const [openAdd, setOpenAdd] = useState(false);

  const fetchClients = async () => {
    try {
      const { data } = await axios.get('http://localhost:8080/api/clients');
      setClients(data);
    } catch (error) {
      console.error('Erreur lors du chargement des clients', error);
    }
  };

  useEffect(() => {
    fetchClients();
  }, []);

  const resetForm = () => {
    setFormData({ nom: '', telephone: '', email: '' });
  };

  const handleEdit = (client) => {
    setEditingClient(client);
    setFormData({
      nom: client.nom || '',
      telephone: client.telephone || '',
      email: client.email || '',
    });
  };

  const handleCancel = () => {
    setEditingClient(null);
    resetForm();
  };

  const handleSave = async () => {
    try {
      const updated = { ...editingClient, ...formData };
      await axios.put(`http://localhost:8080/api/clients/${editingClient.id}`, updated);
      setEditingClient(null);
      fetchClients();
    } catch (error) {
      console.error('Erreur lors de la mise à jour du client', error);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Confirmer la suppression de ce client ?')) return;
    try {
      await axios.delete(`http://localhost:8080/api/clients/${id}`);
      fetchClients();
    } catch (error) {
      console.error('Erreur lors de la suppression', error);
    }
  };

  const handleAddClient = async () => {
    try {
      await axios.post('http://localhost:8080/api/clients', formData);
      setOpenAdd(false);
      resetForm();
      fetchClients();
    } catch (error) {
      console.error('Erreur lors de la création du client', error);
    }
  };

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', backgroundColor: '#fff5f5' }}>
      <Sidebar />
      <Box sx={{ flex: 1, p: 4 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
          <Typography level="h2" sx={{ color: '#b71c1c' }}>
            Gestion des clients
          </Typography>
          <Button startDecorator={<AddIcon />} variant="solid" color="danger" onClick={() => { resetForm(); setOpenAdd(true); }}>
            Ajouter
          </Button>
        </Box>

        <Table variant="outlined" borderAxis="both" hoverRow>
          <thead>
            <tr style={{ backgroundColor: '#ffebee' }}>
              <th>Nom</th>
              <th>Téléphone</th>
              <th>Email</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {clients.map((client) => (
              <tr key={client.id}>
                <td>
                  {editingClient?.id === client.id ? (
                    <input
                      value={formData.nom}
                      onChange={(e) => setFormData({ ...formData, nom: e.target.value })}
                      className="border rounded px-2 py-1"
                    />
                  ) : (
                    client.nom
                  )}
                </td>
                <td>
                  {editingClient?.id === client.id ? (
                    <input
                      value={formData.telephone}
                      onChange={(e) => setFormData({ ...formData, telephone: e.target.value })}
                      className="border rounded px-2 py-1"
                    />
                  ) : (
                    client.telephone
                  )}
                </td>
                <td>
                  {editingClient?.id === client.id ? (
                    <input
                      value={formData.email}
                      onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                      className="border rounded px-2 py-1"
                    />
                  ) : (
                    client.email
                  )}
                </td>
                <td>
                  {editingClient?.id === client.id ? (
                    <>
                      <Button size="sm" color="success" onClick={handleSave}>Enregistrer</Button>
                      <Button size="sm" color="neutral" onClick={handleCancel}>Annuler</Button>
                    </>
                  ) : (
                    <>
                      <IconButton onClick={() => handleEdit(client)} color="primary"><EditIcon /></IconButton>
                      <IconButton onClick={() => handleDelete(client.id)} color="danger"><DeleteIcon /></IconButton>
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      </Box>

      {/* Add Client Modal */}
      <Modal open={openAdd} onClose={() => setOpenAdd(false)}>
        <ModalDialog sx={{ width: 400 }}>
          <Typography level="h4" component="h2" mb={2}>
            Ajouter un client
          </Typography>
          <TextField
            label="Nom"
            fullWidth
            value={formData.nom}
            onChange={(e) => setFormData({ ...formData, nom: e.target.value })}
            sx={{ mb: 1 }}
          />
          <TextField
            label="Téléphone"
            fullWidth
            value={formData.telephone}
            onChange={(e) => setFormData({ ...formData, telephone: e.target.value })}
            sx={{ mb: 1 }}
          />
          <TextField
            label="Email"
            fullWidth
            value={formData.email}
            onChange={(e) => setFormData({ ...formData, email: e.target.value })}
            sx={{ mb: 2 }}
          />
          <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
            <Button color="neutral" onClick={() => setOpenAdd(false)}>Annuler</Button>
            <Button color="danger" onClick={handleAddClient}>Créer</Button>
          </Box>
        </ModalDialog>
      </Modal>
    </Box>
  );
} 