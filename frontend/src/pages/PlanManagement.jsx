import React, { useEffect, useState } from 'react';
import axios from 'axios';
import Box from '@mui/joy/Box';
import Typography from '@mui/joy/Typography';
import Table from '@mui/joy/Table';
import IconButton from '@mui/joy/IconButton';
import Button from '@mui/joy/Button';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import VisibilityIcon from '@mui/icons-material/Visibility';
import AssignmentIndIcon from '@mui/icons-material/AssignmentInd';
import HistoryIcon from '@mui/icons-material/History';
import Modal from '@mui/joy/Modal';
import ModalDialog from '@mui/joy/ModalDialog';
import TextField from '@mui/joy/TextField';
import Select from '@mui/joy/Select';
import Option from '@mui/joy/Option';
import Sidebar from '../components/Sidebar';

export default function PlanManagement() {
  const [plans, setPlans] = useState([]);
  const [selectedPlan, setSelectedPlan] = useState(null);
  const [agents, setAgents] = useState([]);
  const [zones, setZones] = useState([]);
  const [assignModalOpen, setAssignModalOpen] = useState(false);
  const [assignData, setAssignData] = useState({ agentId: '', zoneId: '' });
  const [logsModalOpen, setLogsModalOpen] = useState(false);
  const [logs, setLogs] = useState([]);
  const [logsLoading, setLogsLoading] = useState(false);

  const fetchPlans = async () => {
    try {
      const { data } = await axios.get('http://localhost:8080/api/plans');
      setPlans(data);
    } catch (err) {
      console.error('Erreur de chargement des plans', err);
    }
  };

  const fetchAgentsAndZones = async () => {
    try {
      const [agentRes, zoneRes] = await Promise.all([
        axios.get('http://localhost:8080/users/agents'),
        axios.get('http://localhost:8080/Zone/all'),
      ]);
      setAgents(agentRes.data);
      setZones(zoneRes.data);
    } catch (err) {
      console.error('Erreur de chargement des agents/zones', err);
    }
  };

  useEffect(() => {
    fetchPlans();
  }, []);

  const handleDelete = async (id) => {
    if (!window.confirm('Supprimer ce plan ?')) return;
    try {
      await axios.delete(`http://localhost:8080/api/plans/${id}`);
      fetchPlans();
    } catch (err) {
      console.error('Erreur suppression', err);
    }
  };

  const openAssignModal = async (plan) => {
    setSelectedPlan(plan);
    await fetchAgentsAndZones();
    setAssignModalOpen(true);
  };

  const handleAssign = async () => {
    try {
      const { agentId, zoneId } = assignData;
      if (!agentId || !zoneId || !selectedPlan) return;
      await axios.post(`http://localhost:8080/api/plans/${selectedPlan.id}/agents/${agentId}/assignations`, {
        id: zoneId,
      });
      alert('Agent assigné avec succès');
      setAssignModalOpen(false);
    } catch (err) {
      console.error('Erreur assignation', err);
      alert("Erreur lors de l'assignation");
    }
  };

  const openLogsModal = async (plan) => {
    setSelectedPlan(plan);
    setLogs([]);
    setLogsModalOpen(true);
    setLogsLoading(true);
    try {
      const [scanRes, manuelRes] = await Promise.all([
        axios.get(`http://localhost:8080/checkups/plan/${plan.id}/type/SCAN`),
        axios.get(`http://localhost:8080/checkups/plan/${plan.id}/type/MANUEL`),
      ]);
      const combined = [...scanRes.data, ...manuelRes.data];
      setLogs(combined);
    } catch (err) {
      console.error('Erreur chargement logs', err);
    } finally {
      setLogsLoading(false);
    }
  };

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <Sidebar />
      <Box sx={{ flex: 1, p: 4 }}>
        <Typography level="h2" sx={{ mb: 2 }}>Gestion des Plans</Typography>
        <Table variant="outlined" borderAxis="both" hoverRow>
          <thead>
            <tr>
              <th>Nom</th>
              <th>Date début</th>
              <th>Date fin</th>
              <th>Statut</th>
              <th>Type</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {plans.map((plan) => (
              <tr key={plan.id}>
                <td>{plan.nom}</td>
                <td>{new Date(plan.dateDebut).toLocaleString()}</td>
                <td>{new Date(plan.dateFin).toLocaleString()}</td>
                <td>{plan.statut}</td>
                <td>{plan.type}</td>
                <td>
                  <IconButton color="primary" onClick={() => alert(JSON.stringify(plan, null, 2))}>
                    <VisibilityIcon />
                  </IconButton>
                  <IconButton color="warning" onClick={() => alert('Fonctionnalité de modification à implémenter') }>
                    <EditIcon />
                  </IconButton>
                  <IconButton color="danger" onClick={() => handleDelete(plan.id)}>
                    <DeleteIcon />
                  </IconButton>
                  <IconButton color="success" onClick={() => openAssignModal(plan)}>
                    <AssignmentIndIcon />
                  </IconButton>
                  <IconButton color="neutral" onClick={() => openLogsModal(plan)}>
                    <HistoryIcon />
                  </IconButton>
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      </Box>

      {/* Modal Assignation */}
      <Modal open={assignModalOpen} onClose={() => setAssignModalOpen(false)}>
        <ModalDialog sx={{ width: 400 }}>
          <Typography level="h4" component="h2" mb={2}>
            Assigner un agent
          </Typography>
          <Select
            placeholder="Choisir un agent"
            value={assignData.agentId}
            onChange={(e, value) => setAssignData((prev) => ({ ...prev, agentId: value }))}
            sx={{ mb: 2 }}
          >
            {agents.map((agent) => (
              <Option key={agent.id} value={agent.id}>{agent.firstName} {agent.lastName}</Option>
            ))}
          </Select>
          <Select
            placeholder="Choisir une zone"
            value={assignData.zoneId}
            onChange={(e, value) => setAssignData((prev) => ({ ...prev, zoneId: value }))}
            sx={{ mb: 2 }}
          >
            {zones.map((zone) => (
              <Option key={zone.id} value={zone.id}>{zone.name}</Option>
            ))}
          </Select>
          <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
            <Button color="neutral" onClick={() => setAssignModalOpen(false)}>Annuler</Button>
            <Button color="primary" onClick={handleAssign}>Assigner</Button>
          </Box>
        </ModalDialog>
      </Modal>

      {/* Logs Modal */}
      <Modal open={logsModalOpen} onClose={() => setLogsModalOpen(false)}>
        <ModalDialog sx={{ width: 600, maxHeight: '80vh', overflowY: 'auto' }}>
          <Typography level="h4" mb={2}>Logs du plan {selectedPlan?.nom}</Typography>
          {logsLoading ? (
            <Typography>Chargement...</Typography>
          ) : logs.length === 0 ? (
            <Typography>Aucun log trouvé.</Typography>
          ) : (
            <Table hoverRow variant="outlined" borderAxis="both">
              <thead>
                <tr>
                  <th>Type</th>
                  <th>Date</th>
                  <th>Agent</th>
                  <th>Recomptage ?</th>
                </tr>
              </thead>
              <tbody>
                {logs.map((log) => (
                  <tr key={log.id}>
                    <td>{log.type}</td>
                    <td>{new Date(log.dateCheck).toLocaleString()}</td>
                    <td>{log.agent?.nom} {log.agent?.prenom}</td>
                    <td>{log.demandeRecomptage ? 'Oui' : 'Non'}</td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
          <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 2 }}>
            <Button onClick={() => setLogsModalOpen(false)}>Fermer</Button>
          </Box>
        </ModalDialog>
      </Modal>
    </Box>
  );
} 