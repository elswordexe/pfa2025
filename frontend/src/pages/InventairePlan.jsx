import React, { useEffect, useState } from 'react';
import axios from 'axios';
import {
  Typography,
  Select,
  Option,
  Card,
  CircularProgress,
  Input,
  Button,
  Stack,
  Modal,
  ModalDialog,
  Divider,
  Checkbox,
  Chip,
  FormControl,
  FormLabel,
  Alert,
  Grid,
  Box // Add Box import
} from '@mui/joy';
import Sidebarsuper from '../components/Sidebar';
import { SearchRounded as SearchIcon, CloseRounded } from '@mui/icons-material';
import DeleteIcon from '@mui/icons-material/Delete';
import { useNavigate } from 'react-router-dom';

const InventairePlan = () => {
  const navigate = useNavigate();
  // Add error state
  const [error, setError] = useState(null);
    const [plan, setPlan] = useState({
    nom: '',
    dateDebut: '',
    dateFin: '',
    type: 'COMPLET',
    zones: [],
    produits: [],
    statut: 'Indefini'
  });

  const [selectedAgents, setSelectedAgents] = useState({});
  const [agents, setAgents] = useState([]);
  const [zones, setZones] = useState([]);
  const [loading, setLoading] = useState(false);
  const [step, setStep] = useState(1); // 1: Plan, 2: Assignation, 3: Confirmation

  const [productFilters, setProductFilters] = useState({
    categories: [],
    searchTerm: '',
    selectedProducts: []
  });

  const [allProducts, setAllProducts] = useState([]);
  const [filteredProducts, setFilteredProducts] = useState([]);

  useEffect(() => {
    fetchZonesAndAgents();
    fetchProducts();
  }, []);
  
  const fetchZonesAndAgents = async () => {
    try {
      const [zonesRes, agentsRes] = await Promise.all([
        fetch('http://localhost:8080/Zone/all'),
        fetch('http://localhost:8080/users/agents')
      ]);
      
      const zonesData = await zonesRes.json();
      const agentsData = await agentsRes.json();
      
      setZones(zonesData);
      setAgents(agentsData);
    } catch (error) {
      console.error('Error fetching data:', error);
      setAgents([]);
    }
  };

  const fetchProducts = async () => {
    try {
      const response = await fetch('http://localhost:8080/produits');
      const data = await response.json();
      setAllProducts(data);
      setFilteredProducts(data);
    } catch (error) {
      console.error('Error fetching products:', error);
    }
  };

  // 1. Création du plan
 // Fonction handleSubmit corrigée
const handleSubmit = async (e) => {
  e.preventDefault();
  setLoading(true);
  const token = localStorage.getItem('token');
  const decoded = token ? JSON.parse(atob(token.split('.')[1])) : null;
  const userId = decoded?.id;
  setError(null);

  try { 
    const requestData = {
  nom: plan.nom,
  dateDebut: plan.dateDebut,
  dateFin: plan.dateFin,
  type: plan.type,
  zones: plan.zones.map(zoneId => ({
    id: typeof zoneId === 'string' ? parseInt(zoneId) : zoneId
  })),
  produits: plan.produits.map(produitId => ({
    id: typeof produitId === 'string' ? parseInt(produitId) : produitId
  })),
  statut: "Indefini",
  createur: { id: userId }
};
      const jsonData = JSON.stringify(requestData, null, 2);
      console.log('Données envoyées (format exact Postman):', jsonData);
      const response = await fetch('http://localhost:8080/api/plans', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': '*/*'
        },
        body: jsonData
    }); 
      const responseData = await response.json();
      console.log('Réponse du serveur:', responseData);

      if (!response.ok) {
        throw new Error(responseData.message || `Erreur HTTP ${response.status}`);
      }

      if (responseData.id) {
        console.log(`Plan créé avec succès, ID: ${responseData.id}`);
        // Stocker l'ID du plan si nécessaire
        setPlan(prev => ({ ...prev, id: responseData.id }));
        setStep(3);
      } else {
        throw new Error('Le plan n\'a pas été créé correctement');
      }

  } catch (error) {
    console.error('Erreur:', error);
    setError(error.message || 'Une erreur est survenue lors de la création du plan');
  } finally {
    setLoading(false);
  }
};

  const handleProductSelection = async (selectedProductIds) => {
    setPlan(prev => ({
      ...prev,
      produits: [...selectedProductIds]
    }));
    setProductFilters(prev => ({
      ...prev,
      selectedProducts: selectedProductIds
    }));
  };

  // Add this effect after handleSubmit
  useEffect(() => {
    if (plan.type === 'COMPLET') {
      setPlan(prev => ({
        ...prev,
        inclusTousProduits: true
      }));
    }
  }, [plan.type]);

  const renderStep = () => {
    switch(step) {
      case 1:
        return (
          <Stack spacing={3}>
            <Grid container spacing={2}>
              {/* Remove 'item' prop from Grid components */}
              <Grid xs={12} md={6}>
                <FormControl className="bg-white rounded-lg p-4 border border-gray-200 hover:border-blue-500 transition-colors">
                  <FormLabel className="text-gray-700 font-medium mb-2">Nom du plan</FormLabel>
                  <Input
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 
                      focus:border-blue-500 bg-white"
                    value={plan.nom}
                    onChange={(e) => setPlan({ ...plan, nom: e.target.value })}
                    required
                  />
                </FormControl>
              </Grid>
              <Grid xs={12} md={6}>
                <FormControl className="bg-white rounded-lg p-4 border border-gray-200 hover:border-blue-500 transition-colors">
                  <FormLabel className="text-gray-700 font-medium mb-2">Type d'inventaire</FormLabel>
                  <Select
                    className="w-full bg-white border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    value={plan.type}
                    onChange={(e, value) => {
                      setPlan(prev => ({
                        ...prev,
                        type: value,
                        inclusTousProduits: value === 'COMPLET' ? true : prev.inclusTousProduits
                      }));
                    }}
                  >
                    <Option value="COMPLET">Complet</Option>
                    <Option value="PARTIEL">Partiel</Option>
                    <Option value="TOURNANT">Tournant</Option>
                  </Select>
                </FormControl>
              </Grid>
            </Grid>

            {/* Add after the type d'inventaire Grid and before the dates Grid */}
            <Grid container spacing={2} sx={{ mt: 2 }}>
              <Grid xs={12}>
                <FormControl>
                  <FormLabel>Sélection des zones</FormLabel>
                  <Select
                    multiple
                    value={plan.zones}
                    onChange={(e, newValue) => setPlan({ ...plan, zones: newValue })}
                    renderValue={(selected) => (
                      <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
                        {selected.map((zoneId) => {
                          const zone = zones.find(z => z.id === parseInt(zoneId));
                          return (
                            <Chip
                              key={`zone-${zoneId}`} // Add unique key
                              variant="soft"
                              color="primary"
                              size="sm"
                            >
                              {zone?.name || ''}
                            </Chip>
                          );
                        })}
                      </Box>
                    )}
                    slotProps={{
                      listbox: {
                        sx: { maxHeight: 200, overflow: 'auto' }
                      }
                    }}
                  >
                    {zones.map((zone) => (
                      <Option key={zone.id} value={zone.id}>
                        {zone.name}
                      </Option>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
            </Grid>

            {/* Add after the zones selection Grid and before the dates Grid */}
            <Grid container spacing={2} sx={{ mt: 2 }}>
              <Grid xs={12}>
                <Card variant="outlined" sx={{ p: 2, display: plan.zones.length ? 'block' : 'none' }}>
                  <Typography level="title-sm" sx={{ mb: 1 }}>
                    Zones sélectionnées ({plan.zones.length}):
                  </Typography>
                  <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                    {plan.zones.map((zoneId) => {
                      const zone = zones.find(z => z.id === zoneId);
                      return (
                        <Chip
                          key={zoneId}
                          variant="soft"
                          color="primary"
                          size="md"
                          onClick={(e) => {
                            e.stopPropagation();
                            setPlan(prev => ({
                              ...prev,
                              zones: prev.zones.filter(id => id !== zoneId)
                            }));
                          }}
                          endDecorator={<CloseRounded />}
                          className="bg-blue-100 text-blue-800 border border-blue-200 px-2 py-1 rounded-full text-sm font-medium"
                        >
                          {zone?.name}
                        </Chip>
                      );
                    })}
                  </Stack>
                
                </Card>
              </Grid>
            </Grid>

            <Grid container spacing={2} sx={{ mt: 2 }}>
              <Grid xs={12} md={6}>
                <FormControl>
                  <FormLabel>Date de début</FormLabel>
                  <Input
                    type="datetime-local"
                    value={plan.dateDebut}
                    onChange={(e) => setPlan({ ...plan, dateDebut: e.target.value })}
                    required
                    slotProps={{
                      input: {
                        min: new Date().toISOString().slice(0, 16)
                      }
                    }}
                  />
                </FormControl>
              </Grid>
              <Grid xs={12} md={6}>
                <FormControl>
                  <FormLabel>Date de fin</FormLabel>
                  <Input
                    type="datetime-local"
                    value={plan.dateFin}
                    onChange={(e) => setPlan({ ...plan, dateFin: e.target.value })}
                    required
                    slotProps={{
                      input: {
                        min: plan.dateDebut || new Date().toISOString().slice(0, 16)
                      }
                    }}
                  />
                </FormControl>
              </Grid>
            </Grid>

            {/* Separate FormControl for product selection */}
            <FormLabel>Sélection des produits</FormLabel>
            <Stack spacing={2}>
              {plan.type !== 'COMPLET' && (
                <FormControl>
                  <Checkbox
                    checked={plan.inclusTousProduits}
                    onChange={(e) => setPlan(prev => ({
                      ...prev,
                      inclusTousProduits: e.target.checked
                    }))}
                    label="Inclure tous les produits des zones sélectionnées"
                  />
                </FormControl>
              )}
              
              {plan.type !== 'COMPLET' && !plan.inclusTousProduits && (
                <>
                  <FormControl>
                    <Input
                      placeholder="Rechercher des produits..."
                      value={productFilters.searchTerm}
                      onChange={(e) => {
                        const searchTerm = e.target.value.toLowerCase();
                        setProductFilters(prev => ({
                          ...prev,
                          searchTerm
                        }));
                        const productsInSelectedZones = allProducts.filter(product =>
                          product.zones.some(zone => plan.zones.includes(zone.id))
                        );
                        setFilteredProducts(
                          productsInSelectedZones.filter(product =>
                            product.nom.toLowerCase().includes(searchTerm) ||
                            product.codeBarre.toLowerCase().includes(searchTerm)
                          )
                        );
                      }}
                      endDecorator={<SearchIcon />}
                    />
                  </FormControl>

                  <FormControl>
                    <Select
                      multiple
                      value={plan.produits}
                      onChange={(e, newValue) => {
                        setPlan(prev => ({
                          ...prev,
                          produits: newValue
                        }));
                        handleProductSelection(newValue);
                      }}
                      renderValue={(selected) => (
                        <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
                          {selected.map((productId) => {
                            const product = allProducts.find(p => p.id === productId);
                            return (
                              <Chip
                                key={productId}
                                variant="soft"
                                color="primary"
                                size="sm"
                                className="bg-blue-100 text-blue-800 border border-blue-200 px-2 py-1 rounded-full text-sm font-medium"
                              >
                                {product?.nom}
                              </Chip>
                            );
                          })}
                       
                      
                       </Box>
                      )}
                    >
                      {filteredProducts.map((product) => (
                        <Option key={product.id} value={product.id}>
                          {product.nom} - {product.codeBarre}
                        </Option>
                      ))}
                    </Select>
                  </FormControl>
                </>
              )}
            </Stack>
            
            {/* Add validation for the Submit button */}
            <Button 
              color="primary"
              onClick={() => setStep(2)}
              disabled={
                !plan.nom || 
                !plan.dateDebut || 
                !plan.dateFin || 
                new Date(plan.dateFin) <= new Date(plan.dateDebut) ||
                plan.zones.length === 0 // Add this condition
              }
            >
              Suivant : Assignation des Agents
            </Button>
          </Stack>
        );        case 2:
        return (
          <Stack spacing={3}>
            <Typography level="h5">Assignation des Agents aux Zones</Typography>            {plan.zones.map(zoneId => {
              const zone = zones.find(z => z.id === zoneId);
              return (
                <FormControl key={zoneId}>
                  <FormLabel>{zone?.name}</FormLabel>
                  <Select
                    value={selectedAgents[zoneId] || ''}
                    onChange={(e, value) => setSelectedAgents({
                      ...selectedAgents,
                      [zoneId]: value
                    })}
                  >
                    {agents.map(agent => (
                      <Option 
                        key={agent.id} 
                        value={agent.id}
                      >
                        {`${agent.firstName} ${agent.lastName}`}
                      </Option>
                    ))}
                  </Select>
                </FormControl>
              );
            })}
          </Stack>
        );
    case 3:
      return (
        <div className="text-center py-12">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-green-100 mb-6">
            <svg className="w-8 h-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <h3 className="text-xl font-semibold text-gray-800 mb-2">Plan d'inventaire créé avec succès!</h3>
          <p className="text-gray-600">Les notifications ont été envoyées aux agents assignés.</p>
          <button
            onClick={() => window.location.href = '/inventory'}
            className="mt-6 px-6 py-3 bg-gradient-to-r from-blue-600 to-indigo-700 text-white rounded-lg 
              hover:from-blue-700 hover:to-indigo-800 shadow-md transition inline-flex items-center gap-2"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5l7 7-7 7" />
            </svg>
            Voir les plans d'inventaire
          </button>
        </div>
      );
    }
  };

  return (
    <div className="flex h-screen overflow-hidden">
      <Sidebarsuper />
      <div className="flex-1 flex flex-col overflow-hidden bg-gradient-to-br from-blue-50 to-indigo-50">
        <div className="flex-1 overflow-auto">
          <div className="min-h-full p-4 md:p-6">
            {/* Header Section */}
            <div className="bg-white rounded-2xl shadow-xl">
              <div className="p-6 border-b border-gray-200">
                <div className="flex flex-col md:flex-row justify-between items-center">
                  <div>
                    <h1 className="text-2xl md:text-3xl font-bold text-gray-800 flex items-center">
                      <svg className="w-8 h-8 mr-3 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                        <path d="M8 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z" />
                        <path d="M6 3a2 2 0 00-2 2v11a2 2 0 002 2h8a2 2 0 002-2V5a2 2 0 00-2-2 3 3 0 01-3 3H9a3 3 0 01-3-3z" />
                      </svg>
                      {step === 1 ? "Créer un Plan d'Inventaire" : 
                       step === 2 ? 'Assigner les Agents' : 
                       'Plan Créé'}
                    </h1>
                    <p className="text-gray-600 mt-2">
                      {step === 1 ? "Définissez les paramètres du plan" :
                       step === 2 ? "Assignez les agents aux zones sélectionnées" :
                       "Configuration terminée"}
                    </p>
                  </div>
                </div>

                {/* Progress Steps */}
                <div className="mt-6">
                  <div className="flex items-center justify-center">
                    {[1, 2, 3].map((stepNumber) => (
                      <div key={stepNumber} className="flex items-center">
                        <div className={`w-8 h-8 rounded-full flex items-center justify-center ${
                          stepNumber <= step 
                            ? 'bg-blue-600 text-white' 
                            : 'bg-gray-200 text-gray-600'
                        }`}>
                          {stepNumber}
                        </div>
                        {stepNumber < 3 && (
                          <div className={`w-20 h-1 ${
                            stepNumber < step ? 'bg-blue-600' : 'bg-gray-200'
                          }`} />
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              </div>

              {/* Main Content Area */}
              <div className="p-6">
                <div className="bg-white rounded-xl border border-gray-200">
                  {renderStep()}
                </div>

                {/* Actions */}
                {step !== 3 && (
                  <div className="flex justify-end gap-3 mt-6">
                    {step > 1 && (
                      <button
                        onClick={() => setStep(step - 1)}
                        className="px-5 py-2.5 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition"
                      >
                        Retour
                      </button>
                    )}
                    <button
                      onClick={step === 1 ? () => setStep(2) : handleSubmit}
                      disabled={loading || !plan.nom || !plan.dateDebut || !plan.dateFin || plan.zones.length === 0}
                      className="px-5 py-2.5 bg-gradient-to-r from-blue-600 to-indigo-700 text-white rounded-lg 
                        hover:from-blue-700 hover:to-indigo-800 shadow-md transition disabled:opacity-50 
                        disabled:cursor-not-allowed flex items-center gap-2"
                    >
                      {loading ? (
                        <span className="inline-block w-4 h-4 border-2 border-white/20 border-t-white rounded-full animate-spin" />
                      ) : null}
                      {step === 1 ? "Suivant" : "Créer le Plan"}
                    </button>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default InventairePlan;
