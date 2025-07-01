import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import { getUserIdFromToken } from '../utils/auth';
import Sidebar from '../components/Sidebar';

const AgentInventaire = () => {
  const [assignedPlans, setAssignedPlans] = useState([]);
  const [selectedPlan, setSelectedPlan] = useState(null);
  const [selectedZone, setSelectedZone] = useState(null);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [manualQuantity, setManualQuantity] = useState('');
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [recomptages, setRecomptages] = useState([]);
  const navigate = useNavigate();
  const userId = getUserIdFromToken();

  useEffect(() => {
    fetchAssignedPlans();
  }, []);

  const fetchAssignedPlans = async () => {
    try {
      setLoading(true);
      const response = await axios.get(`http://localhost:8080/api/plans/assigned/${userId}`);
      setAssignedPlans(response.data);
    } catch (error) {
      console.error('Error fetching assigned plans:', error);
      toast.error('Error fetching assigned plans');
    } finally {
      setLoading(false);
    }
  };

  const fetchZoneProducts = async (planId, zoneId) => {
    try {
      const response = await axios.get(`http://localhost:8080/api/plans/${planId}/zone-products`);
      setProducts(response.data);
    } catch (error) {
      console.error('Error fetching zone products:', error);
      toast.error('Erreur lors du chargement des produits');
    }
  };

  const fetchRecomptages = async (planId) => {
    try {
      const response = await axios.get(`http://localhost:8080/checkups/plan/${planId}/recomptages`);
      setRecomptages(response.data);
    } catch (error) {
      console.error('Error fetching recomptages:', error);
    }
  };

  const handlePlanSelect = async (plan) => {
    setSelectedPlan(plan);
    setSelectedZone(null);
    setProducts([]);
    await fetchRecomptages(plan.id);
  };

  const handleZoneSelect = async (zone) => {
    setSelectedZone(zone);
    if (selectedPlan) {
      await fetchZoneProducts(selectedPlan.id, zone.id);
    }
  };

  const handleManualCount = async (product) => {
    if (!manualQuantity || isNaN(manualQuantity)) {
      toast.error('Veuillez entrer une quantité valide');
      return;
    }

    try {
      await axios.post(`http://localhost:8080/checkups/manual`, {
        planId: selectedPlan.id,
        zoneId: selectedZone.id,
        produitId: product.id,
        quantity: Number(manualQuantity)
      });

      toast.success('Quantité manuelle enregistrée');
      setManualQuantity('');
      await fetchZoneProducts(selectedPlan.id, selectedZone.id);
    } catch (error) {
      console.error('Error saving manual count:', error);
      toast.error('Erreur lors de l\'enregistrement');
    }
  };

  const handleScan = async (product) => {
    try {
      await axios.post(`http://localhost:8080/checkups/scan`, {
        planId: selectedPlan.id,
        zoneId: selectedZone.id,
        produitId: product.id,
        quantity: 1
      });

      toast.success('Scan enregistré');
      await fetchZoneProducts(selectedPlan.id, selectedZone.id);
    } catch (error) {
      console.error('Error recording scan:', error);
      toast.error('Erreur lors du scan');
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-screen bg-gray-100">
        <Sidebar />
        <div className="flex-1 p-8">
          <div className="flex justify-center items-center h-full">
            <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500"></div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen bg-gray-100">
      <Sidebar />
      <div className="flex-1 p-8">
        <ToastContainer />
        
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-gray-800">Interface Agent Inventaire</h1>
          <p className="text-gray-600">Gérez vos plans d'inventaire assignés</p>
        </div>

        {/* Plans Section */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-lg font-semibold mb-4">Plans Assignés</h2>
            <div className="space-y-2">
              {assignedPlans.map(plan => (
                <button
                  key={plan.id}
                  onClick={() => handlePlanSelect(plan)}
                  className={`w-full p-3 rounded-lg text-left ${
                    selectedPlan?.id === plan.id
                      ? 'bg-blue-500 text-white'
                      : 'bg-gray-100 hover:bg-gray-200'
                  }`}
                >
                  <div className="font-medium">{plan.nom}</div>
                  <div className="text-sm">
                    {new Date(plan.dateDebut).toLocaleDateString()} - {new Date(plan.dateFin).toLocaleDateString()}
                  </div>
                </button>
              ))}
            </div>
          </div>

          {selectedPlan && (
            <div className="bg-white rounded-lg shadow p-6">
              <h2 className="text-lg font-semibold mb-4">Zones Assignées</h2>
              <div className="space-y-2">
                {selectedPlan.assignations
                  .filter(assignation => assignation.agent.id === userId)
                  .map(assignation => (
                    <button
                      key={assignation.zone.id}
                      onClick={() => handleZoneSelect(assignation.zone)}
                      className={`w-full p-3 rounded-lg text-left ${
                        selectedZone?.id === assignation.zone.id
                          ? 'bg-blue-500 text-white'
                          : 'bg-gray-100 hover:bg-gray-200'
                      }`}
                    >
                      {assignation.zone.name}
                    </button>
                  ))}
              </div>
            </div>
          )}

          {recomptages.length > 0 && (
            <div className="bg-white rounded-lg shadow p-6">
              <h2 className="text-lg font-semibold mb-4 text-red-600">
                Demandes de Recomptage
              </h2>
              <div className="space-y-4">
                {recomptages.map(recomptage => (
                  <div
                    key={recomptage.id}
                    className="p-4 bg-red-50 rounded-lg border border-red-200"
                  >
                    <div className="font-medium text-red-700">
                      {recomptage.produit.nom}
                    </div>
                    <div className="text-sm text-red-600">
                      Zone: {recomptage.zone.name}
                    </div>
                    <div className="text-sm text-gray-600 mt-2">
                      Justification: {recomptage.justificationRecomptage}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Products Section */}
        {selectedZone && (
          <div className="bg-white rounded-lg shadow overflow-hidden">
            <div className="p-6">
              <h2 className="text-lg font-semibold mb-4">
                Produits de la Zone: {selectedZone.name}
              </h2>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Produit
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Code Barre
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Qté Théorique
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Saisie Manuelle
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {products.map(product => (
                    <tr key={product.id}>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm font-medium text-gray-900">
                          {product.nom}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-gray-500">
                          {product.codeBarre}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-gray-900">
                          {product.quantiteTheorique}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <input
                          type="number"
                          min="0"
                          value={selectedProduct?.id === product.id ? manualQuantity : ''}
                          onChange={(e) => {
                            setSelectedProduct(product);
                            setManualQuantity(e.target.value);
                          }}
                          className="w-24 px-2 py-1 border rounded"
                          placeholder="Qté"
                        />
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex space-x-2">
                          <button
                            onClick={() => handleManualCount(product)}
                            className="bg-blue-500 text-white px-3 py-1 rounded hover:bg-blue-600"
                          >
                            Valider Manuel
                          </button>
                          <button
                            onClick={() => handleScan(product)}
                            className="bg-green-500 text-white px-3 py-1 rounded hover:bg-green-600"
                          >
                            Scanner
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default AgentInventaire; 