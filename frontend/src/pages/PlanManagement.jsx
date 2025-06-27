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
import { useNavigate } from 'react-router-dom';
import dayjs from 'dayjs';
import Stack from '@mui/joy/Stack';
import FormControl from '@mui/joy/FormControl';
import FormLabel from '@mui/joy/FormLabel';
import Input from '@mui/joy/Input';
import Chip from '@mui/joy/Chip';

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
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editPlanData, setEditPlanData] = useState({ nom: '', dateDebut: '', dateFin: '', type: '' });
  const [assignations, setAssignations] = useState({}); 

  const navigate = useNavigate();

  // Fetch assignments for all plans
  const fetchAssignationsForPlans = async (plansList) => {
    const result = {};
    await Promise.all(
      plansList.map(async (plan) => {
        try {
          const { data } = await axios.get(`http://localhost:8080/api/plans/${plan.id}/assignations`);
          result[plan.id] = data;
        } catch {
          result[plan.id] = [];
        }
      })
    );
    setAssignations(result);
  };

  const fetchPlans = async () => {
    try {
      const { data } = await axios.get('http://localhost:8080/api/plans');
      const list = Array.isArray(data) ? data : (Array.isArray(data?.content) ? data.content : []);
      setPlans(list);
      await fetchAssignationsForPlans(list);
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
      const token = localStorage.getItem('token');
      const authHeaders = token ? { Authorization: `Bearer ${token}` } : {};

      await axios.post(`http://localhost:8080/api/plans/${selectedPlan.id}/agents/${agentId}/assignations`, {
        id: zoneId,
      }, { headers: { ...authHeaders, 'Content-Type': 'application/json' } });
      alert('Agent assigné avec succès');
      setAssignModalOpen(false);
      
      const { data } = await axios.get(`http://localhost:8080/api/plans/${selectedPlan.id}/assignations`, { headers: authHeaders });
      setAssignations((prev) => ({ ...prev, [selectedPlan.id]: data }));
    } catch (err) {
      console.error('Erreur assignation', err);
      alert("Erreur lors de l'assignation");
    }
  };

  // Ajouter cette fonction pour regrouper les logs
  const groupLogs = (logs) => {
    const groupedMap = new Map();

    logs.forEach(log => {
      if (!log.details || log.details.length === 0) return;

      const detail = log.details[0];
      if (!detail.produit || !detail.zone) return;

      // Utiliser une clé qui identifie uniquement le checkup et le produit
      const key = `${detail.produit.id}_${detail.zone.id}_${new Date(log.dateCheck).getTime()}`;
      
      if (!groupedMap.has(key)) {
        groupedMap.set(key, {
          id: log.id,
          dateCheck: log.dateCheck,
          agent: log.agent,
          produit: detail.produit,
          zone: detail.zone,
          demandeRecomptage: log.demandeRecomptage,
          scannedQuantity: null,
          manualQuantity: null,
          justificationRecomptage: log.justificationRecomptage
        });
      }

      const groupedLog = groupedMap.get(key);
      if (detail.scannedQuantity !== null && detail.scannedQuantity !== undefined) {
        groupedLog.scannedQuantity = detail.scannedQuantity;
      }
      if (detail.manualQuantity !== null && detail.manualQuantity !== undefined) {
        groupedLog.manualQuantity = detail.manualQuantity;
      }
    });

    return Array.from(groupedMap.values())
      .sort((a, b) => new Date(b.dateCheck) - new Date(a.dateCheck));
  };

  const openLogsModal = async (plan) => {
    setSelectedPlan(plan);
    setLogs([]);
    setLogsModalOpen(true);
    setLogsLoading(true);
    try {
      const token = localStorage.getItem('token');
      const authHeaders = token ? { Authorization: `Bearer ${token}` } : {};
      const { data } = await axios.get(`http://localhost:8080/checkups/plan/${plan.id}/logs`, { headers: authHeaders });
      setLogs(groupLogs(Array.isArray(data) ? data : []));
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
              <th>Agents assignés</th>
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
                  {(assignations[plan.id] || []).length === 0
                    ? <span style={{ color: '#aaa' }}>Aucun</span>
                    : Array.from(
                        new Set(
                          (assignations[plan.id] || [])
                            .filter(a => a.agent)
                            .map(a => `${a.agent.nom || ''} ${a.agent.prenom || ''}`.trim())
                        )
                      ).join(', ')
                  }
                </td>
                <td>
                  <IconButton color="primary" onClick={() => {
                    localStorage.setItem('selectedPlanId', plan.id);
                    navigate('/inventory');
                  }}>
                    <VisibilityIcon />
                  </IconButton>
                  <IconButton color="warning" onClick={() => {
                    setSelectedPlan(plan);
                    setEditPlanData({
                      ...plan,
                      dateDebut: dayjs(plan.dateDebut).format('YYYY-MM-DDTHH:mm'),
                      dateFin: dayjs(plan.dateFin).format('YYYY-MM-DDTHH:mm'),
                      // Ensure statut, produits, zones, assignations, etc. are present
                      statut: plan.statut || '',
                      produits: plan.produits || [],
                      zones: plan.zones || [],
                      assignations: plan.assignations || [],
                    });
                    setEditModalOpen(true);
                  }}>
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
              <Option key={agent.id} value={agent.id}>{agent.nom} {agent.prenom}</Option>
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

      <Modal open={logsModalOpen} onClose={() => setLogsModalOpen(false)}>
        <ModalDialog sx={{ width: 800, maxHeight: '80vh', overflowY: 'auto' }}>
          <Typography level="h4" mb={2}>Historique du plan {selectedPlan?.nom}</Typography>
          {logsLoading ? (
            <Typography>Chargement...</Typography>
          ) : logs.length === 0 ? (
            <Typography>Aucun log trouvé.</Typography>
          ) : (
            <Table hoverRow variant="outlined" borderAxis="both">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Agent</th>
                  <th>Produit</th>
                  <th>Zone</th>
                  <th style={{ textAlign: 'center' }}>
                    <i className="material-icons" style={{fontSize: '16px', marginRight: '4px'}}>qr_code_scanner</i>
                    Scan
                  </th>
                  <th style={{ textAlign: 'center' }}>
                    <i className="material-icons" style={{fontSize: '16px', marginRight: '4px'}}>edit</i>
                    Manuel
                  </th>
                  <th>État</th>
                </tr>
              </thead>
              <tbody>
                {logs.map((log) => (
                  <tr key={`${log.id}_${log.produit.id}_${log.zone.id}`} className="hover:bg-blue-50 transition">
                    <td>{new Date(log.dateCheck).toLocaleString()}</td>
                    <td>
                      {log.agent ? (
                        <Chip
                          size="sm"
                          variant="soft"
                          color="primary"
                        >
                          {`${log.agent.nom || ''} ${log.agent.prenom || ''}`.trim()}
                        </Chip>
                      ) : '-'}
                    </td>
                    <td>
                      {log.produit ? (
                        <Stack spacing={1}>
                          <Typography level="body-sm">{log.produit.nom || '-'}</Typography>
                          <Typography level="body-xs" sx={{ color: 'text.secondary' }}>
                            {log.produit.codeBarre || '-'}
                          </Typography>
                        </Stack>
                      ) : '-'}
                    </td>
                    <td>
                      {log.zone ? (
                        <Chip
                          size="sm"
                          variant="soft"
                          color="neutral"
                        >
                          {log.zone.name || '-'}
                        </Chip>
                      ) : '-'}
                    </td>
                    <td style={{ textAlign: 'center' }}>
                      {log.scannedQuantity !== null && log.scannedQuantity !== undefined ? (
                        <Chip
                          size="sm"
                          variant="soft"
                          color="success"
                        >
                          {log.scannedQuantity}
                        </Chip>
                      ) : '-'}
                    </td>
                    <td style={{ textAlign: 'center' }}>
                      {log.manualQuantity !== null && log.manualQuantity !== undefined ? (
                        <Chip
                          size="sm"
                          variant="soft"
                          color="warning"
                        >
                          {log.manualQuantity}
                        </Chip>
                      ) : '-'}
                    </td>
                    <td>
                      {log.demandeRecomptage ? (
                        <Stack spacing={1}>
                          <Chip
                            size="sm"
                            color="danger"
                            variant="soft"
                            startDecorator={<i className="material-icons">refresh</i>}
                          >
                            Recomptage demandé
                          </Chip>
                          {log.justificationRecomptage && (
                            <Typography level="body-xs" sx={{ color: 'text.secondary' }}>
                              Justification: {log.justificationRecomptage}
                            </Typography>
                          )}
                        </Stack>
                      ) : (
                        <Chip
                          size="sm"
                          color="success"
                          variant="soft"
                          startDecorator={<i className="material-icons">check</i>}
                        >
                          Validé
                        </Chip>
                      )}
                    </td>
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

      <Modal open={editModalOpen} onClose={() => setEditModalOpen(false)}>
        <ModalDialog sx={{ width: 400 }}>
          <Typography level="h4" mb={2}>Modifier le plan</Typography>
          <Stack spacing={2}>
            <FormControl>
              <FormLabel>Nom</FormLabel>
              <Input value={editPlanData.nom} onChange={(e)=>setEditPlanData(prev=>({...prev, nom:e.target.value}))} />
            </FormControl>
            <FormControl>
              <FormLabel>Date début</FormLabel>
              <Input type="datetime-local" value={editPlanData.dateDebut} onChange={(e)=>setEditPlanData(prev=>({...prev, dateDebut:e.target.value}))}/>
            </FormControl>
            <FormControl>
              <FormLabel>Date fin</FormLabel>
              <Input type="datetime-local" value={editPlanData.dateFin} onChange={(e)=>setEditPlanData(prev=>({...prev, dateFin:e.target.value}))}/>
            </FormControl>
            <Select placeholder="Type" value={editPlanData.type} onChange={(e,val)=>setEditPlanData(prev=>({...prev,type:val}))}>
              <Option value="COMPLET">Complet</Option>
              <Option value="PARTIEL">Partiel</Option>
              <Option value="TOURNANT">Tournant</Option>
            </Select>
          </Stack>
          <Box sx={{ display:'flex',justifyContent:'flex-end',gap:1, mt:2 }}>
            <Button onClick={()=>setEditModalOpen(false)}>Annuler</Button>
            <Button onClick={async ()=>{
              try{
                await axios.patch(`http://localhost:8080/api/plans/${selectedPlan.id}`, {
                  nom: editPlanData.nom,
                  dateDebut: editPlanData.dateDebut,
                  dateFin: editPlanData.dateFin,
                  type: editPlanData.type,
                });
                setEditModalOpen(false);
                fetchPlans();
              }catch(err){
                console.error('Erreur maj plan',err);
                alert('Erreur lors de la mise à jour');
              }
            }}>Enregistrer</Button>
          </Box>
        </ModalDialog>
      </Modal>
    </Box>
  );
}