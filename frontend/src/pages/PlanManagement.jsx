import React, { useEffect, useState } from 'react';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import VisibilityIcon from '@mui/icons-material/Visibility';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import HistoryIcon from '@mui/icons-material/History';
import axios from 'axios';
import Sidebar from '../components/Sidebar';
import { useNavigate } from 'react-router-dom';
import dayjs from 'dayjs';


export default function PlanManagement() {
  const [plans, setPlans] = useState([]);
  const [selectedPlan, setSelectedPlan] = useState(null);
  const [agents, setAgents] = useState([]);
  const [zones, setZones] = useState([]);
  const [assignModalOpen, setAssignModalOpen] = useState(false);
  const [assignData, setAssignData] = useState({ agentId: '', zoneIds: [], produitsParZone: {} });
  const [zoneProducts, setZoneProducts] = useState({});
  const [logsModalOpen, setLogsModalOpen] = useState(false);
  const [logs, setLogs] = useState([]);
  const [logsLoading, setLogsLoading] = useState(false);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editPlanData, setEditPlanData] = useState({ nom: '', dateDebut: '', dateFin: '', type: '' });
  const [assignations, setAssignations] = useState({});
  const navigate = useNavigate();

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
    setAssignData({ agentId: '', zoneIds: [], produitsParZone: {} });
    setZoneProducts({});
    setAssignModalOpen(true);
  };

  const handleAssign = async () => {
    try {
      const { agentId } = assignData;
      if (!agentId || !selectedPlan) return;
      const token = localStorage.getItem('token');
      const authHeaders = token ? { Authorization: `Bearer ${token}` } : {};
      await axios.patch(`http://localhost:8080/api/plans/${selectedPlan.id}/agents/${agentId}/assignations`, {}, { headers: { ...authHeaders, 'Content-Type': 'application/json' } });

      alert('Agent assigné avec succès');
      setAssignModalOpen(false);
      const { data } = await axios.get(`http://localhost:8080/api/plans/${selectedPlan.id}/assignations`, { headers: authHeaders });
      setAssignations((prev) => ({ ...prev, [selectedPlan.id]: data }));
    } catch (err) {
      console.error('Erreur assignation', err);
      alert("Erreur lors de l'assignation");
    }
  };

  const groupLogs = (logs) => {
    const groupedMap = new Map();

    logs.forEach(log => {
      if (!log.details || log.details.length === 0) return;

      const detail = log.details[0];
      if (!detail.produit || !detail.zone) return;

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
    <div className="flex min-h-screen bg-gradient-to-br from-blue-50 to-indigo-50">
      <Sidebar />
      <div className="flex-1 min-w-0 overflow-x-hidden">
        <div className="p-4 md:p-6">
          <div className="bg-white rounded-2xl shadow-xl p-6 mb-6 border border-gray-200">
            <div className="flex flex-col md:flex-row justify-between items-center mb-6">
              <h1 className="text-2xl md:text-3xl font-bold text-gray-800 flex items-center">
                <svg className="w-8 h-8 mr-3 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M8 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z" />
                  <path d="M6 3a2 2 0 00-2 2v11a2 2 0 002 2h8a2 2 0 002-2V5a2 2 0 00-2-2 3 3 0 01-3 3H9a3 3 0 01-3-3z" />
                </svg>
                Gestion des Plans
              </h1>
            </div>
            <div className="overflow-hidden rounded-xl border border-gray-200 shadow-sm">
              <table className="w-full">
                <thead className="bg-gradient-to-r from-blue-600 to-indigo-700 text-white">
                  <tr>
                    <th className="p-4 text-left">Nom</th>
                    <th className="p-4 text-left">Date début</th>
                    <th className="p-4 text-left">Date fin</th>
                    <th className="p-4 text-left">Statut</th>
                    <th className="p-4 text-left">Type</th>
                    <th className="p-4 text-left">Zones concernées</th>
                    <th className="p-4 text-left">Agents assignés</th>
                    <th className="p-4 text-left">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {plans.map((plan) => (
                    <tr key={plan.id} className="border-b border-gray-200 hover:bg-blue-50">
                      <td className="p-4 text-gray-900">{plan.nom}</td>
                      <td className="p-4 text-gray-900">{new Date(plan.dateDebut).toLocaleString()}</td>
                      <td className="p-4 text-gray-900">{new Date(plan.dateFin).toLocaleString()}</td>
                      <td className="p-4 text-blue-700">{plan.statut}</td>
                      <td className="p-4 text-blue-700">{plan.type}</td>
                      <td className="p-4 text-blue-900">
                        {(plan.zones && plan.zones.length > 0)
                          ? plan.zones.map(z => z.name || z.nom || `Zone #${z.id}`).join(', ')
                          : <span className="text-gray-400">Aucune</span>
                        }
                      </td>
                      <td className="p-4 text-blue-900">
                        {(assignations[plan.id] || []).length === 0
                          ? <span className="text-gray-400">Aucun</span>
                          : Array.from(
                              new Set(
                                (assignations[plan.id] || [])
                                  .filter(a => a.agent)
                                  .map(a => {
                                    const fullName = `${a.agent.firstName || ''} ${a.agent.lastName || ''}`.trim();
                                    return fullName || `Agent #${a.agent.id}`;
                                  })
                              )
                            ).join(', ')
                        }
                      </td>
                      <td className="p-4 flex gap-1">
                        <Tooltip title="Voir" arrow>
                          <IconButton
                            color="primary"
                            size="small"
                            onClick={() => {
                              localStorage.setItem('selectedPlanId', plan.id);
                              navigate('/inventory');
                            }}
                          >
                            <VisibilityIcon />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Éditer" arrow>
                          <IconButton
                            sx={{ color: '#FFD600' }}
                            size="small"
                            onClick={() => {
                              setSelectedPlan(plan);
                              setEditPlanData({
                                ...plan,
                                dateDebut: dayjs(plan.dateDebut).format('YYYY-MM-DDTHH:mm'),
                                dateFin: dayjs(plan.dateFin).format('YYYY-MM-DDTHH:mm'),
                                statut: plan.statut || '',
                                produits: plan.produits || [],
                                zones: plan.zones || [],
                                assignations: plan.assignations || [],
                              });
                              setEditModalOpen(true);
                            }}
                          >
                            <EditIcon />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Supprimer" arrow>
                          <IconButton
                            sx={{ color: '#D32F2F' }}
                            size="small"
                            onClick={() => handleDelete(plan.id)}
                          >
                            <DeleteIcon />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Assigner" arrow>
                          <IconButton
                            sx={{ color: '#388E3C' }}
                            size="small"
                            onClick={() => openAssignModal(plan)}
                          >
                            <PersonAddIcon />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Historique" arrow>
                          <IconButton
                            sx={{ color: '#616161' }}
                            size="small"
                            onClick={() => openLogsModal(plan)}
                          >
                            <HistoryIcon />
                          </IconButton>
                        </Tooltip>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
      {/* Modals */}
      {assignModalOpen && (
        <div className="fixed inset-0 bg-white bg-opacity-50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
            <div className="bg-gradient-to-r from-blue-600 to-indigo-700 p-5">
              <h3 className="text-xl font-semibold text-white flex items-center">
                Assigner un agent
              </h3>
            </div>
            <div className="p-6">
              <label className="block text-gray-700 mb-2">Choisir un agent</label>
              <select
                className="w-full p-2 border border-gray-300 rounded-lg mb-4"
                value={assignData.agentId}
                onChange={e => setAssignData(prev => ({ ...prev, agentId: e.target.value }))}
              >
                <option value="">Sélectionner...</option>
                {agents.map(agent => (
                  <option key={agent.id} value={agent.id}>
                    {`${agent.firstName || ''} ${agent.lastName || ''}`.trim() || `Agent #${agent.id}`}
                  </option>
                ))}
              </select>
              <div className="flex justify-end gap-3">
                <button
                  className="px-5 py-2.5 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-100 transition"
                  onClick={() => setAssignModalOpen(false)}
                >Annuler</button>
                <button
                  className="px-5 py-2.5 bg-gradient-to-r from-blue-600 to-indigo-700 text-white rounded-lg hover:from-blue-700 hover:to-indigo-800 shadow-md transition"
                  onClick={handleAssign}
                >Assigner</button>
              </div>
            </div>
          </div>
        </div>
      )}
      {logsModalOpen && (
        <div className="fixed inset-0 bg-white bg-opacity-90 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-4xl overflow-hidden">
            <div className="bg-gradient-to-r from-blue-600 to-indigo-700 p-5">
              <h3 className="text-xl font-semibold text-white flex items-center">
                Historique du plan {selectedPlan?.nom}
              </h3>
            </div>
            <div className="p-6 max-h-[60vh] overflow-y-auto">
              {logsLoading ? (
                <div className="text-center text-gray-600">Chargement...</div>
              ) : logs.length === 0 ? (
                <div className="text-center text-gray-600">Aucun log trouvé.</div>
              ) : (
                <table className="w-full">
                  <thead className="bg-blue-100">
                    <tr>
                      <th className="p-2 text-left">Date</th>
                      <th className="p-2 text-left">Agent</th>
                      <th className="p-2 text-left">Produit</th>
                      <th className="p-2 text-left">Zone</th>
                      <th className="p-2 text-center">Scan</th>
                      <th className="p-2 text-center">Manuel</th>
                      <th className="p-2 text-left">État</th>
                    </tr>
                  </thead>
                  <tbody>
                    {logs.map((log) => (
                      <tr key={`${log.id}_${log.produit.id}_${log.zone.id}`} className="hover:bg-blue-50 transition">
                        <td className="p-2">{new Date(log.dateCheck).toLocaleString()}</td>
                        <td className="p-2">
                          {log.agent ? (
                            <span className="inline-block bg-blue-100 text-blue-800 px-2 py-1 rounded text-xs font-medium">
                              {`${log.agent.nom || ''} ${log.agent.prenom || ''}`.trim()}
                            </span>
                          ) : '-'}
                        </td>
                        <td className="p-2">
                          {log.produit ? (
                            <div>
                              <div className="font-medium">{log.produit.nom || '-'}</div>
                              <div className="text-xs text-gray-500">{log.produit.codeBarre || '-'}</div>
                            </div>
                          ) : '-'}
                        </td>
                        <td className="p-2">
                          {log.zone ? (
                            <span className="inline-block bg-gray-100 text-gray-800 px-2 py-1 rounded text-xs font-medium">
                              {log.zone.name || '-'}
                            </span>
                          ) : '-'}
                        </td>
                        <td className="p-2 text-center">
                          {log.scannedQuantity !== null && log.scannedQuantity !== undefined ? (
                            <span className="inline-block bg-green-100 text-green-800 px-2 py-1 rounded text-xs font-medium">
                              {log.scannedQuantity}
                            </span>
                          ) : '-'}
                        </td>
                        <td className="p-2 text-center">
                          {log.manualQuantity !== null && log.manualQuantity !== undefined ? (
                            <span className="inline-block bg-yellow-100 text-yellow-800 px-2 py-1 rounded text-xs font-medium">
                              {log.manualQuantity}
                            </span>
                          ) : '-'}
                        </td>
                        <td className="p-2">
                          {log.demandeRecomptage ? (
                            <span className="inline-block bg-red-100 text-red-800 px-2 py-1 rounded text-xs font-medium">
                              Recomptage demandé
                              {log.justificationRecomptage && (
                                <span className="block text-xs text-gray-500 mt-1">Justification: {log.justificationRecomptage}</span>
                              )}
                            </span>
                          ) : (
                            <span className="inline-block bg-green-100 text-green-800 px-2 py-1 rounded text-xs font-medium">
                              Validé
                            </span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
            <div className="flex justify-end gap-3 p-6 bg-gray-50">
              <button
                className="px-5 py-2.5 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-100 transition"
                onClick={() => setLogsModalOpen(false)}
              >Fermer</button>
            </div>
          </div>
        </div>
      )}
      {editModalOpen && (
        <div className="fixed inset-0 bg-white bg-opacity-50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
            <div className="bg-gradient-to-r from-blue-600 to-indigo-700 p-5">
              <h3 className="text-xl font-semibold text-white flex items-center">
                Modifier le plan
              </h3>
            </div>
            <div className="p-6 space-y-4">
              <div>
                <label className="block text-gray-700 mb-1">Nom</label>
                <input
                  className="w-full p-2 border border-gray-300 rounded-lg"
                  value={editPlanData.nom}
                  onChange={e => setEditPlanData(prev => ({ ...prev, nom: e.target.value }))}
                />
              </div>
              <div>
                <label className="block text-gray-700 mb-1">Date début</label>
                <input
                  type="datetime-local"
                  className="w-full p-2 border border-gray-300 rounded-lg"
                  value={editPlanData.dateDebut}
                  onChange={e => setEditPlanData(prev => ({ ...prev, dateDebut: e.target.value }))}
                />
              </div>
              <div>
                <label className="block text-gray-700 mb-1">Date fin</label>
                <input
                  type="datetime-local"
                  className="w-full p-2 border border-gray-300 rounded-lg"
                  value={editPlanData.dateFin}
                  onChange={e => setEditPlanData(prev => ({ ...prev, dateFin: e.target.value }))}
                />
              </div>
              <div>
                <label className="block text-gray-700 mb-1">Type</label>
                <select
                  className="w-full p-2 border border-gray-300 rounded-lg"
                  value={editPlanData.type}
                  onChange={e => setEditPlanData(prev => ({ ...prev, type: e.target.value }))}
                >
                  <option value="COMPLET">Complet</option>
                  <option value="PARTIEL">Partiel</option>
                  <option value="TOURNANT">Tournant</option>
                </select>
              </div>
              <div className="flex justify-end gap-3">
                <button
                  className="px-5 py-2.5 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-100 transition"
                  onClick={() => setEditModalOpen(false)}
                >Annuler</button>
                <button
                  className="px-5 py-2.5 bg-gradient-to-r from-blue-600 to-indigo-700 text-white rounded-lg hover:from-blue-700 hover:to-indigo-800 shadow-md transition"
                  onClick={async () => {
                    try {
                      await axios.patch(`http://localhost:8080/api/plans/${selectedPlan.id}`, {
                        nom: editPlanData.nom,
                        dateDebut: editPlanData.dateDebut,
                        dateFin: editPlanData.dateFin,
                        type: editPlanData.type,
                      });
                      setEditModalOpen(false);
                      fetchPlans();
                    } catch (err) {
                      console.error('Erreur maj plan', err);
                      alert('Erreur lors de la mise à jour');
                    }
                  }}
                >Enregistrer</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}