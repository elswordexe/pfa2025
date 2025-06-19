import React, { useEffect, useState } from 'react';
import Sidebarsuper from '../components/Sidebarsuper';
import axios from 'axios';
import { useTheme, useMediaQuery } from '@mui/material';
import { toast, ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import InventoryPDF from '../components/InventoryPDF';

const ITEMS_PER_PAGE = 5;

const Inventory = () => {
  const [plans, setPlans] = useState([]);
  const [manualCheckups, setManualCheckups] = useState([]);
  const [scanCheckups, setScanCheckups] = useState([]);
  const [loading, setLoading] = useState(true);
  const [planId, setPlanId] = useState(1);
  const [availablePlans, setAvailablePlans] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [filter, setFilter] = useState('');
  const [error, setError] = useState(null);
  const [planproducts, setPlanProducts] = useState([]);
  const [showJustificationModal, setShowJustificationModal] = useState(false);
  const [selectedCheckupId, setSelectedCheckupId] = useState(null);
  const [justification, setJustification] = useState('');
  const [selectedZone, setSelectedZone] = useState('');
  const [zones, setZones] = useState([]);
  const [activeTab, setActiveTab] = useState('products'); 
  const [showPDF, setShowPDF] = useState(false);
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [products, setProducts] = useState([]);
  const[ZoneProducts, setZoneProducts] = useState([]);
  const[ProdcutStatus, setProductStatus] = useState([]);
  const [planDetails, setPlanDetails] = useState({});
  const [selectedProduct, setSelectedProduct] = useState(null);
 useEffect(() => {
  const fetchPlans = async () => {
    try {
      const response = await axios.get('http://localhost:8080/api/plans');
      const plans = Array.isArray(response.data) ? response.data : [response.data];

      setAvailablePlans(prevPlans => {
        const newData = JSON.stringify(plans);
        const oldData = JSON.stringify(prevPlans);
        return newData !== oldData ? plans : prevPlans;
      });
    } catch (error) {
      console.error('Error fetching plans:', error);
      setAvailablePlans([]); // Only if you want to clear it on error
      setError('Failed to load plans');
    }
  };
   fetchPlans();
  const interval = setInterval(fetchPlans, 5000);
  return () => clearInterval(interval);
}, []);

  useEffect(() => {    const fetchZonesAndPlan = async () => {
      setLoading(true);
      try {
        // 1. Fetch all zones first
        const zonesRes = await axios.get('http://localhost:8080/Zone/all');
        console.log('All zones:', zonesRes.data);
        const allZones = zonesRes.data || [];

        // 2. Fetch plan details
        const planRes = await axios.get(`http://localhost:8080/api/plans/${planId}/details`);
        console.log('Plan details:', planRes.data); 
        // 3. Fetch zone products avec les quantités théoriques
        const zoneProductsRes = await axios.get(`http://localhost:8080/api/plans/${planId}/zone-products`);
        
        // Log détaillé pour debug
        console.log('Zone products raw response:', zoneProductsRes.data);
        
        // Vérifions la structure exacte des données de Zone_Produit
        const firstZone = Object.keys(zoneProductsRes.data || {})[0];
        const firstProduct = zoneProductsRes.data[firstZone]?.[0];
        
        console.log('Structure détaillée:', {
          firstZoneId: firstZone,
          firstProductInZone: firstProduct,
          quantityFields: firstProduct ? {
            quantite: firstProduct.quantite,
            quantite_theorique: firstProduct.quantite_theorique,
            quantitetheo: firstProduct.quantitetheo,
            quantiteTheorique: firstProduct.quantiteTheorique,
            Zone_Produit: firstProduct.Zone_Produit
          } : null
        });
        console.log('API Response - Plan Data:', planRes.data);
        console.log('API Response - Zone Products:', zoneProductsRes.data);
        
        const planData = planRes.data || { zones: [] };
        const zones = allZones;
        const zoneProducts = zoneProductsRes.data || {};

        console.log('Zones récupérées:', zones);
    

        const productsWithZones = Object.entries(zoneProducts).reduce((acc, [zoneId, products = []]) => {
          products.forEach(product => {            const existingProduct = acc.find(p => p.id === product.id);
            const zoneObj = zones.find(z => z.id === Number(zoneId));
            
            const zoneWithProducts = zones.find(z => z.id === Number(zoneId));
            
            // Chercher la quantité théorique dans zoneProduits
            const zoneProduit = zoneWithProducts?.zoneProduits?.find(zp => 
              zp.id.produitId === product.id && zp.id.zoneId === Number(zoneId)
            );
            
            const quantiteTheo = zoneProduit?.quantiteTheorique || 
                               product.quantiteTheorique ||
                               0;
            
            console.log('Quantité extraite pour produit:', {
              productId: product.id,
              zoneId: zoneId,
              zoneWithProducts,
              zoneProduit,
              quantiteTheo,
              fullProduct: product
            });

            if (existingProduct) {
              existingProduct.zones.push(zoneObj);
              existingProduct.zoneQuantities = {
                ...existingProduct.zoneQuantities,
                [zoneId]: Number(quantiteTheo)
              };
            } else {
              acc.push({
                ...product,
                zones: [zoneObj].filter(Boolean),
                zoneQuantities: { [zoneId]: Number(quantiteTheo) }
              });
            }
          });
          return acc;
        }, []);

        console.log('Produits avec leurs zones:', productsWithZones);
        productsWithZones.forEach(product => {
          console.log('Product zones details:', {
            productId: product.id,
            productName: product.nom,
            zones: product.zones.map(z => ({
              id: z?.id,
              nom: z?.nom,
              name: z?.name
            }))
          });
        });

        setPlanProducts(productsWithZones);
        setZones(zones);

        // 3. Fetch checkups
        const [manualRes, scanRes] = await Promise.all([
          axios.get(`http://localhost:8080/checkups/plan/${planId}/type/MANUEL`),
          axios.get(`http://localhost:8080/checkups/plan/${planId}/type/SCAN`)
        ]);

        setManualCheckups(manualRes.data || []);
        setScanCheckups(scanRes.data || []);
      } catch (error) {
        console.error('Error fetching data:', error);
        setError(error.message);
      } finally {
        setLoading(false);
      }
    };

    fetchZonesAndPlan();
    const interval = setInterval(fetchZonesAndPlan, 5000); // fetch every 5s
    return () => clearInterval(interval);
  }, [planId]);

  const refreshCheckups = async () => {
    try {
      const [manualRes, scanRes] = await Promise.all([
        axios.get(`http://localhost:8080/checkups/plan/${planId}/type/MANUEL`),
        axios.get(`http://localhost:8080/checkups/plan/${planId}/type/SCAN`)
      ]);
      setManualCheckups(manualRes.data);
      setScanCheckups(scanRes.data);
    } catch {
      alert("Erreur lors du rechargement");
    }
  };

  const handleRaccomptage = async (checkupId) => {
    setSelectedCheckupId(checkupId);
    setShowJustificationModal(true);
  };

  const submitRecomptage = async () => {
    if (!justification.trim()) {
      toast.error("Veuillez fournir une justification");
      return;
    }

    try {
      toast.info('Traitement de la demande de recomptage...', {
        toastId: 'recomptage-start'
      });

    
      await axios.put(`http://localhost:8080/checkups/${selectedCheckupId}/recomptage`, {
        demandeRecomptage: true,
        justification: justification
      });

  
      const checkup = [...manualCheckups, ...scanCheckups].find(c => c.id === selectedCheckupId);
      if (checkup && checkup.produitId) {
       
        const zone = checkup.zoneId || selectedZone;
        await axios.put(`http://localhost:8080/produits/${checkup.produitId}/zones/${zone}/updateQuantite`, {
          quantiteManuelle: 0,
          quantiteScannee: 0,
          status: 'A_RECOMPTER'
        });

        toast.success('Quantités réinitialisées', {
          toastId: 'quantities-reset'
        });
      }

      await refreshCheckups();
      await refreshData();

      setShowJustificationModal(false);
      setJustification('');
      setSelectedCheckupId(null);

      toast.success('Recomptage initié avec succès', {
        toastId: 'recomptage-success'
      });
    } catch (error) {
      console.error('Error during recount process:', error);
      toast.error("Erreur lors de la demande de recomptage", {
        toastId: 'recomptage-error'
      });
    }
  };  const handleValider = async (checkupId, produitId, scannedQty, manualQty, theoreticalQty) => {
    const scanned = Number(scannedQty);
    const manual = Number(manualQty);
    const theoretical = Number(theoreticalQty);
    const currentZone = selectedZone || (planproducts.find(p => p.id === produitId)?.zones[0]?.id);

    if (!currentZone) {
      toast.error('Zone non trouvée');
      return;
    }

    if (scanned !== manual) {
      try {
        if (isNaN(manual)) {
          toast.error('La quantité manuelle n\'est pas un nombre valide');
          return;
        }
        
        toast.info('Validation en cours...', {
          toastId: 'validating'
        });

        const quantiteTheorique = Number(manual);
        console.log('Sending update with quantities:', {
          manual,
          quantiteTheorique,
          currentZone
        });

        await axios.put(`http://localhost:8080/produits/${produitId}/zones/${currentZone}/updateQuantite`, {
          quantiteTheorique: quantiteTheorique,
          SetS: 'VERIFIE'
        });
        
        toast.success('Quantité théorique mise à jour', {
          toastId: 'quantity-updated'
        });

        // Valider le check seulement si checkupId existe
        if (checkupId) {
          await axios.put(`http://localhost:8080/checkups/${checkupId}/valider`);
          toast.success('Contrôle validé', {
            toastId: 'checkup-validated'
          });
        }

        await refreshData(); // Recharger toutes les données
      } catch (error) {
        console.error('Error updating quantities:', error);
        toast.error('Erreur lors de la mise à jour');
      }
    } else {
      if (!window.confirm("Confirmer la validation de ce produit ?")) return;
      try {
        toast.info('Validation en cours...', {
          toastId: 'validating'
        });

        // Mise à jour de la quantité théorique et du statut
        await axios.put(`http://localhost:8080/produits/${produitId}/zones/${currentZone}/updateQuantite`, {
          quantiteTheorique: Number(manual),
          setProductStatus: 'VERIFIE'
        });
        
        toast.success('Quantité théorique mise à jour', {
          toastId: 'quantity-updated'
        });

        // Valider le check seulement si checkupId existe
        if (checkupId) {
          await axios.put(`http://localhost:8080/checkups/${checkupId}/valider`);
          toast.success('Contrôle validé', {
            toastId: 'checkup-validated'
          });
        }

        await refreshData(); // Recharger toutes les données
      } catch (error) {
        console.error('Error validating checkup:', error);
        toast.error('Erreur lors de la validation');
      }
    }
  };

  const getQuantityColor = (scannedQty, manualQty, theoreticalQty) => {
    const scanned = Number(scannedQty);
    const manual = Number(manualQty);
    const theoretical = Number(theoreticalQty);

    if (scanned === manual && manual === theoretical) {
      return 'bg-green-100 text-green-800 border-green-200';
    } else if (scanned !== manual) {
      return 'bg-yellow-100 text-yellow-800 border-yellow-200';
    } else if (scanned !== theoretical || manual !== theoretical) {
      return 'bg-red-100 text-red-800 border-red-200';
    }
    return 'bg-blue-100 text-blue-800 border-blue-200';
  };

  useEffect(() => {
    if (planproducts.length > 0) {
      console.log('Current products:', planproducts);
      console.log('Current zones:', zones);
      console.log('Selected zone:', selectedZone);
    }
  }, [planproducts, zones, selectedZone]);

  const handleExportPDF = () => {
    setShowPDF(true);
  };

  useEffect(() => {
    const initialLoad = async () => {
      if (!planId) return;
      
      try {
        setLoading(true);
        const [productsRes, scanRes, manualRes] = await Promise.all([
          axios.get(`http://localhost:8080/api/produits/plan/${planId}`),
          axios.get(`http://localhost:8080/api/checkups/scan/plan/${planId}`),
          axios.get(`http://localhost:8080/api/checkups/manual/plan/${planId}`)
        ]);

        const productsWithStatus = productsRes.data.map(product => ({
          ...product,
          status: product.status || 'EN_ATTENTE'
        }));

        setPlanProducts(productsWithStatus);
        setScanCheckups(scanRes.data);
        setManualCheckups(manualRes.data);
      } catch (error) {
        console.error('Error fetching inventory data:', error);
        toast.error('Erreur lors du chargement des données');
      } finally {
        setLoading(false);
      }
    };

    initialLoad();
    const interval = setInterval(silentUpdate, 5000);
    return () => clearInterval(interval);
  }, [planId]);

  // Add refresh function
  const refreshData = async () => {
    await silentUpdate();
  };

  const silentUpdate = async () => {
    try {
      const [productsRes, scanRes, manualRes] = await Promise.all([
        axios.get(`http://localhost:8080/api/produits/plan/${planId}`),
        axios.get(`http://localhost:8080/api/checkups/scan/plan/${planId}`),
        axios.get(`http://localhost:8080/api/checkups/manual/plan/${planId}`)
      ]);

      const updatedProducts = productsRes.data.map(product => ({
        ...product,
        status: product.status || 'EN_ATTENTE'
      }));

      setPlanProducts(prevProducts => {
        // Ne mettre à jour que si les données ont changé
        const hasChanged = JSON.stringify(prevProducts) !== JSON.stringify(updatedProducts);
        return hasChanged ? updatedProducts : prevProducts;
      });
      setScanCheckups(prevCheckups => {
        const hasChanged = JSON.stringify(prevCheckups) !== JSON.stringify(scanRes.data);
        return hasChanged ? scanRes.data : prevCheckups;
      });
      setManualCheckups(prevCheckups => {
        const hasChanged = JSON.stringify(prevCheckups) !== JSON.stringify(manualRes.data);
        return hasChanged ? manualRes.data : prevCheckups;
      });
    } catch (error) {
      console.error('Error updating data:', error);
      // Pas de toast d'erreur pour les mises à jour silencieuses
    }
  };

  const getButtonStatus = (product, scannedQty, manualQty) => {
    // Vérifie si le produit a le status VERIFIE
    if (product.status === 'VERIFIE') {
      return {
        validate: { disabled: true, text: 'Vérifié' },
        recount: { disabled: true, text: 'Recomptage indisponible' }
      };
    }

    // Vérifie si le produit est en cours de recomptage
    if (product.status === 'A_RECOMPTER') {
      return {
        validate: { disabled: true, text: 'En recomptage' },
        recount: { disabled: true, text: 'Recomptage en cours' }
      };
    }

    // Vérifie si les quantités manuelles sont disponibles
    const hasManualQty = !isNaN(manualQty) && manualQty !== null && manualQty !== undefined;
    
    return {
      validate: { 
        disabled: !hasManualQty, 
        text: hasManualQty ? 'Valider' : 'Non disponible' 
      },
      recount: {
        disabled: false,
        text: 'Recompter'
      }
    };
  };

  if (loading) {
    return (
      <div className="flex min-h-screen bg-gradient-to-br from-blue-50 to-indigo-50">
        <Sidebarsuper />
        <div className={`p-6 ${isMobile ? 'w-full' : 'ml-60 w-full'}`}>
          <div className="bg-white rounded-xl shadow-lg p-6 max-w-4xl mx-auto">
            <h1 className="text-3xl font-bold text-gray-800 mb-8 text-center flex items-center justify-center">
              <svg className="w-8 h-8 mr-3 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                <path d="M8 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z" />
                <path d="M6 3a2 2 0 00-2 2v11a2 2 0 002 2h8a2 2 0 002-2V5a2 2 0 00-2-2 3 3 0 01-3 3H9a3 3 0 01-3-3z" />
              </svg>
              Suivi des Écarts d'Inventaire
            </h1>
            <div className="flex justify-center mt-10 py-12">
              <div className="animate-spin rounded-full h-12 w-12 border-t-4 border-b-4 border-blue-600" />
              <span className="ml-4 text-gray-600 font-medium">Chargement des données...</span>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex min-h-screen bg-gradient-to-br from-blue-50 to-indigo-50">
        <Sidebarsuper />
        <div className={`p-6 ${isMobile ? 'w-full' : 'ml-60 w-full'}`}>
          <div className="bg-white rounded-xl shadow-lg p-6 max-w-4xl mx-auto">
            <h1 className="text-3xl font-bold text-gray-800 mb-8 text-center flex items-center justify-center">
              <svg className="w-8 h-8 mr-3 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                <path d="M8 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z" />
                <path d="M6 3a2 2 0 00-2 2v11a2 2 0 002 2h8a2 2 0 002-2V5a2 2 0 00-2-2 3 3 0 01-3 3H9a3 3 0 01-3-3z" />
              </svg>
              Suivi des Écarts d'Inventaire
            </h1>
            <div className="text-center p-8 rounded-xl bg-red-50 border border-red-200">
              <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-red-100 mb-4">
                <svg className="w-8 h-8 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
              </div>
              <h3 className="text-xl font-semibold text-red-700 mb-2">Erreur de chargement</h3>
              <p className="text-gray-600">Les données ne sont pas disponibles pour le moment.</p>
              <p className="text-sm text-gray-500 mt-2">Détails: {error}</p>
              <button 
                onClick={() => window.location.reload()}
                className="mt-4 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition flex items-center justify-center mx-auto"
              >
                <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                Réessayer
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }
  return (
    <div className="flex min-h-screen bg-gradient-to-br from-blue-50 to-indigo-50">
      <Sidebarsuper />
      <ToastContainer
        position="top-right"
        autoClose={3000}
        hideProgressBar={false}
        newestOnTop={false}
        closeOnClick
        rtl={false}
        pauseOnFocusLoss
        draggable
        pauseOnHover
        theme="colored"
      />
      
      {/* Update the main content container */}
      <div className="flex-1 min-w-0 overflow-x-hidden"> {/* Added these classes */}
        <div className="p-4 md:p-6">
          <div className="bg-white rounded-2xl shadow-xl p-6 mb-6">
            <div className="flex flex-col md:flex-row justify-between items-center mb-6">
              <div>
                <h1 className="text-2xl md:text-3xl font-bold text-gray-800 flex items-center">
                  <svg className="w-8 h-8 mr-3 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M8 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z" />
                    <path d="M6 3a2 2 0 00-2 2v11a2 2 0 002 2h8a2 2 0 002-2V5a2 2 0 00-2-2 3 3 0 01-3 3H9a3 3 0 01-3-3z" />
                  </svg>
                  Suivi des Écarts d'Inventaire
                </h1>
                <p className="text-gray-600 mt-2">Gestion des écarts et validation des produits</p>
              </div>
              
              <div className="flex flex-wrap gap-3 mt-4 md:mt-0">
                <button
                  onClick={handleExportPDF}
                  className="flex items-center gap-2 bg-gradient-to-r from-blue-600 to-indigo-700 hover:from-blue-700 hover:to-indigo-800 text-white px-4 py-2 rounded-lg shadow-md transition"
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                  Export PDF
                </button>
                <button
                  onClick={() => window.open(`http://localhost:8080/ecarts/export/xlsx?planId=${planId}`, '_blank')}
                  className="flex items-center gap-2 bg-white border border-blue-600 text-blue-600 hover:bg-blue-50 px-4 py-2 rounded-lg shadow-md transition"
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                  Export Excel
                </button>
              </div>
            </div>
            
            <div className="flex flex-wrap gap-4 items-center justify-between mb-6 p-4 bg-blue-50 rounded-xl">
              <div className="flex flex-col md:flex-row gap-4">
                <div className="flex flex-col">
                  <label className="text-sm font-medium text-gray-700 mb-1">Plan d'inventaire</label>
                  <div className="relative">
                    <select
                      value={planId}
                      onChange={(e) => setPlanId(Number(e.target.value))}
                      className="w-full md:w-64 pl-10 pr-4 py-2 rounded-lg border border-gray-300 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white"
                    >
                      {Array.isArray(availablePlans) && availablePlans.length > 0 ? (
                        availablePlans.map(plan => (
                          <option key={plan.id} value={plan.id}>
                            {plan.nom || `Plan ${plan.id}`}
                          </option>
                        ))
                      ) : (
                        <option value="">No plans available</option>
                      )}
                    </select>
                    <div className="absolute left-3 top-2.5 text-blue-600">
                      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                      </svg>
                    </div>
                  </div>
                </div>
                
                <div className="flex flex-col">
                  <label className="text-sm font-medium text-gray-700 mb-1">Filtrer par zone</label>
                  <div className="relative">
                    <select
                      value={selectedZone}
                      onChange={(e) => setSelectedZone(e.target.value)}
                      className="w-full md:w-48 pl-10 pr-4 py-2 rounded-lg border border-gray-300 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white"
                    >
                      <option value="">Toutes les zones</option>
                      {zones.map(zone => (
                        <option key={zone.id} value={zone.id}>
                          {zone.name}
                        </option>
                      ))}
                    </select>
                    <div className="absolute left-3 top-2.5 text-blue-600">
                      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                      </svg>
                    </div>
                  </div>
                </div>
              </div>
              
              <div className="flex flex-col w-full md:w-auto">
                <label className="text-sm font-medium text-gray-700 mb-1">Rechercher un produit</label>
                <div className="relative">
                  <input
                    type="text"
                    placeholder="Nom ou référence..."
                    value={filter}
                    onChange={(e) => setFilter(e.target.value)}
                    className="w-full pl-10 pr-4 py-2 rounded-lg border border-gray-300 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white"
                  />
                  <div className="absolute left-3 top-2.5 text-gray-400">
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                    </svg>
                  </div>
                </div>
              </div>
            </div>
            
            <div className="mb-6">
              <div className="flex border-b border-gray-200">
                <button
                  className={`py-2 px-4 font-medium text-sm border-b-2 -mb-px transition ${
                    activeTab === 'products' 
                      ? 'border-blue-600 text-blue-600' 
                      : 'border-transparent text-gray-500 hover:text-gray-700'
                  }`}
                  onClick={() => setActiveTab('products')}
                >
                  Vue par produit
                </button>
                <button
                  className={`py-2 px-4 font-medium text-sm border-b-2 -mb-px transition ${
                    activeTab === 'zones' 
                      ? 'border-blue-600 text-blue-600' 
                      : 'border-transparent text-gray-500 hover:text-gray-700'
                  }`}
                  onClick={() => setActiveTab('zones')}
                >
                  Vue par zone
                </button>
              </div>
            </div>
            
            {activeTab === 'products' ? (
              <div className="overflow-hidden rounded-xl border border-gray-200 shadow-sm">
                <table className="w-full">
                  <thead className="bg-gradient-to-r from-blue-600 to-indigo-700 text-white">                    <tr>
                      <th className="p-4 text-left">Produit</th>
                      <th className="p-4 text-left">Référence</th>
                      <th className="p-4 text-left">Zone</th>
                      <th className="p-4 text-right">Qté Théorique</th>
                      <th className="p-4 text-right">Manuel</th>
                      <th className="p-4 text-right">Scan</th>
                      <th className="p-4 text-center">Écart</th>
                      <th className="p-4 text-center">Statut</th>
                      <th className="p-4 text-center">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {planproducts
                      .filter(product => !selectedZone || product.zones.some(z => z.id.toString() === selectedZone))
                      .filter(product => 
                        !filter || 
                        product.nom?.toLowerCase().includes(filter.toLowerCase()) ||
                        product.codeBarre?.toLowerCase().includes(filter.toLowerCase())
                      )
                      .map(product => {
                        const scanDetail = scanCheckups.flatMap(s => s.details).find(d => d.produit?.id === product.id);
                        const manualDetail = manualCheckups.flatMap(m => m.details).find(d => d.produit?.id === product.id);
                        const scanned = scanDetail?.scannedQuantity ?? "-";
                        const manual = manualDetail?.scannedQuantity ?? "-";
                        // Get theoretical quantity for current zone if selected, otherwise use total                        // Log des quantités pour debug
                        console.log('Product quantities:', {
                          productId: product.id,
                          productName: product.nom,
                          zoneQuantities: product.zoneQuantities,
                          selectedZone,
                          fullProduct: product
                        });                        const getZoneQuantity = (zoneId) => {
                          const zone = zones.find(z => z.id === Number(zoneId));
                          const zoneProduit = zone?.zoneProduits?.find(zp => 
                            zp.id.produitId === product.id && zp.id.zoneId === Number(zoneId)
                          );
                          return zoneProduit?.quantiteTheorique || 0;
                        };

                        const theoreticalQty = selectedZone 
                          ? getZoneQuantity(selectedZone)
                          : product.zones.reduce((sum, zone) => sum + getZoneQuantity(zone.id), 0);
                        const colorClass = getQuantityColor(scanned, manual, theoreticalQty);
                        const zoneNames = product.zones?.map(z => z?.name).filter(Boolean).join(', ') || '-';

                        return (
                          <tr key={product.id} className="border-b hover:bg-blue-50 transition">                            <td className="p-4 font-medium text-gray-800">{product.nom || '-'}</td>
                            <td className="p-4 text-blue-600 font-mono">{product.codeBarre || '-'}</td>
                            <td className="p-4 text-gray-600">
                              {product.zones && product.zones.length > 0 ? 
                                product.zones.map(zone => {
                                  console.log('Zone in render:', zone);
                                  if (!zone) return '-';
                                  const zoneName = zone.nom || zone.name || zone.designation || '-';
                                  console.log('Zone name used:', zoneName);
                                  return zoneName;
                                }).join(', ') 
                                : '-'}
                            </td>
                            <td className={`p-4 text-right font-medium ${colorClass}`}>
                              {theoreticalQty}
                            </td>
                            <td className={`p-4 text-right font-bold ${colorClass}`}>
                              {manual}
                            </td>
                            <td className={`p-4 text-right font-bold ${colorClass}`}>
                              {scanned}
                            </td>
                            <td className="p-4 text-center">                              <span className={`px-3 py-1 rounded-full text-sm font-medium ${colorClass}`}>
                                {manual !== "-" ? manual - theoreticalQty : "-"}
                              </span>
                            </td>
                            <td className="p-4 text-center">
                              {product.status === 'VERIFIE' ? (
                                <span className="px-3 py-1 bg-green-100 text-green-800 rounded-full text-sm font-medium">
                                  ✓ Vérifié
                                </span>
                              ) : (
                                <span className="px-3 py-1 bg-gray-100 text-gray-600 rounded-full text-sm font-medium">
                                  En attente
                                </span>
                              )}
                            </td>
                            <td className="p-4 text-center">
                              <div className="flex justify-center gap-2">
                                <button
                                  onClick={() => handleValider(
                                    manualDetail?.checkupId,
                                    product.id,
                                    scanned,
                                    manual,
                                    theoreticalQty,
                                  )}
                                  className="flex items-center gap-1 bg-green-600 hover:bg-green-700 text-white px-3 py-1.5 rounded text-sm font-medium transition"
                                >
                                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
                                  </svg>
                                  Valider
                                </button>
                                <button
                                  onClick={() => handleRaccomptage(manualDetail?.checkupId)}
                                  className="flex items-center gap-1 bg-amber-500 hover:bg-amber-600 text-white px-3 py-1.5 rounded text-sm font-medium transition"
                                >
                                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                                  </svg>
                                  Recompter
                                </button>
                              </div>
                            </td>
                          </tr>
                        );
                      })}
                  </tbody>
                </table>
                
                {planproducts.length === 0 && (
                  <div className="text-center py-12">
                    <div className="inline-block p-4 bg-blue-100 rounded-full mb-4">
                      <svg className="w-12 h-12 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
                      </svg>
                    </div>
                    <h3 className="text-lg font-medium text-gray-800 mb-2">Aucun produit trouvé</h3>
                    <p className="text-gray-600">Aucun produit ne correspond à vos critères de recherche</p>
                  </div>
                )}
              </div>
            ) : (
              <div className="space-y-6">
                {zones
                  .filter(zone => !selectedZone || zone.id.toString() === selectedZone)
                  .map(zone => {
                    const zoneProducts = planproducts.filter(product => 
                      product.zones.some(z => z.id === zone.id)
                    );

                    if (zoneProducts.length === 0) return null;

                    return (
                      <div key={zone.id} className="bg-white rounded-xl border border-gray-200 overflow-hidden">
                        <div className="bg-gradient-to-r from-blue-600 to-indigo-700 px-6 py-4">
                          <div className="flex items-center justify-between">
                            <h3 className="text-xl font-semibold text-white flex items-center">
                              <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                              </svg>
                              Zone: {zone.name}
                            </h3>
                            <span className="bg-blue-800 text-blue-100 px-3 py-1 rounded-full text-sm">
                              {zoneProducts.length} {zoneProducts.length > 1 ? "produits" : "produit"}
                            </span>
                          </div>
                        </div>
                        
                        <div className="overflow-x-auto">
                          <table className="w-full">
                            <thead className="bg-gray-100">
                              <tr>
                                <th className="p-3 text-left">Produit</th>
                                <th className="p-3 text-left">Code</th>
                                <th className="p-3 text-right">Qté Théorique</th>
                                <th className="p-3 text-right">Manuel</th>
                                <th className="p-3 text-right">Scan</th>
                                <th className="p-3 text-center">Écart</th>
                                <th className="p-3 text-center">Actions</th>
                              </tr>
                            </thead>
                            <tbody>
                              {zoneProducts
                                .filter(product => !filter || 
                                  product.nom.toLowerCase().includes(filter.toLowerCase()) ||
                                  product.codeBarre.toLowerCase().includes(filter.toLowerCase())
                                )
                                .map(product => {
                                  const scanDetail = scanCheckups.flatMap(s => s.details).find(d => d.produit.id === product.id);
                                  const manualDetail = manualCheckups.flatMap(m => m.details).find(d => d.produit.id === product.id);
                                  const scanned = scanDetail?.scannedQuantity ?? "-";
                                  const manual = manualDetail?.scannedQuantity ?? "-";
                                  const zoneProduit = zone.zoneProduits?.find(zp => 
                                    zp.id.produitId === product.id && zp.id.zoneId === zone.id
                                  );
                                  const theoreticalQty = zoneProduit?.quantiteTheorique || 0;
                                  const colorClass = getQuantityColor(scanned, manual, theoreticalQty);

                                  return (
                                    <tr key={product.id} className="border-b hover:bg-blue-50">
                                      <td className="p-3 font-medium text-gray-800">{product.nom}</td>
                                      <td className="p-3 text-blue-600 font-mono">{product.codeBarre}</td>
                                      <td className={`p-3 text-right font-medium ${colorClass} border-l-4`}>
                                        {theoreticalQty}
                                      </td>
                                      <td className={`p-3 text-right font-bold ${colorClass}`}>
                                        {manual}
                                      </td>
                                      <td className={`p-3 text-right font-bold ${colorClass}`}>
                                        {scanned}
                                      </td>
                                      <td className="p-3 text-center">
                                        <span className={`px-3 py-1 rounded-full text-sm font-medium ${colorClass}`}>
                                          {manual !== "-" ? manual - theoreticalQty : "-"}
                                        </span>
                                      </td>                      <td className="p-3 text-center">
                              <div className="flex justify-center gap-2">                                <button
                                  onClick={() => {
                                    const canValidate = manual || scanned;
                                    if (!canValidate) {
                                      toast.warning('Aucune quantité disponible à valider');
                                      return;
                                    }
                                    handleValider(
                                      manualDetail?.checkupId,
                                      product.id,
                                      scanned,
                                      manual,
                                      theoreticalQty
                                    );
                                  }}
                                  disabled={product.status === 'VERIFIE' || (!manual && !scanned)}
                                  className={`flex items-center gap-1 px-3 py-1.5 rounded text-sm font-medium transition ${
                                    product.status === 'VERIFIE'
                                      ? 'bg-gray-300 cursor-not-allowed text-gray-600'
                                      : (!manual && !scanned)
                                      ? 'bg-gray-400 cursor-not-allowed text-gray-200'
                                      : 'bg-green-600 hover:bg-green-700 text-white'
                                  }`}
                                  title={
                                    product.status === 'VERIFIE'
                                      ? 'Produit déjà vérifié'
                                      : (!manual && !scanned)
                                      ? 'Aucune quantité disponible'
                                      : 'Valider ce produit'
                                  }
                                >
                                  {product.status === 'VERIFIE' ? (
                                    <>
                                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                                      </svg>
                                      Vérifié
                                    </>
                                  ) : (
                                    <>
                                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
                                      </svg>
                                      {!manualDetail?.checkupId ? 'Non disponible' : 'Valider'}
                                    </>
                                  )}
                                </button>
                                          <button
                                            onClick={() => handleRaccomptage(manualDetail?.checkupId)}
                                            className="flex items-center gap-1 bg-amber-500 hover:bg-amber-600 text-white px-3 py-1.5 rounded text-sm font-medium transition"
                                          >
                                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                                            </svg>
                                            Recompter
                                          </button>
                                        </div>
                                      </td>
                                    </tr>
                                  );
                                })}
                            </tbody>
                          </table>
                        </div>
                      </div>
                    );
                  })}
              </div>
            )}
            
            <div className="mt-6 flex justify-between items-center">
              <div className="text-sm text-gray-600">
                {planproducts.length} produits au total
              </div>
              <div className="flex gap-2">
                <button 
                  className="px-3 py-1.5 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-100 disabled:opacity-50"
                  disabled={currentPage === 1}
                  onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
                >
                  Précédent
                </button>
                <button 
                  className="px-3 py-1.5 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-100 disabled:opacity-50"
                  disabled={currentPage * ITEMS_PER_PAGE >= planproducts.length}
                  onClick={() => setCurrentPage(prev => prev + 1)}
                >
                  Suivant
                </button>
              </div>
            </div>
          </div>
          
          {/* Modal de justification */}
          {showJustificationModal && (
            <div className="fixed inset-0 bg-white bg-opacity-50 flex items-center justify-center p-4 z-50">
              <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden">
                <div className="bg-gradient-to-r from-blue-600 to-indigo-700 p-5">
                  <h3 className="text-xl font-semibold text-white flex items-center">
                    <svg className="w-6 h-6 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                    Justification du recomptage
                  </h3>
                </div>
                
                <div className="p-6">
                  <p className="text-gray-600 mb-4">
                    Veuillez expliquer les raisons nécessitant un recomptage pour ce produit.
                  </p>
                  <textarea
                    value={justification}
                    onChange={(e) => setJustification(e.target.value)}
                    className="w-full h-32 p-4 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    placeholder="Décrivez les raisons du recomptage..."
                  />
                </div>
                
                <div className="flex justify-end gap-3 p-6 bg-gray-50">
                  <button
                    onClick={() => {
                      setShowJustificationModal(false);
                      setJustification('');
                    }}
                    className="px-5 py-2.5 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-100 transition"
                  >
                    Annuler
                  </button>
                  <button
                    onClick={submitRecomptage}
                    className="px-5 py-2.5 bg-gradient-to-r from-blue-600 to-indigo-700 text-white rounded-lg hover:from-blue-700 hover:to-indigo-800 shadow-md transition"
                  >
                    Confirmer la demande
                  </button>
                </div>
              </div>
            </div>
          )}
          {showPDF && (
            <div className="fixed inset-0 bg-white bg-opacity-90 flex items-center justify-center p-4 z-50">
              <div className="bg-white rounded-2xl shadow-2xl w-full max-w-4xl overflow-hidden">
                <div className="bg-gradient-to-r from-blue-600 to-indigo-700 p-5">
                  <h3 className="text-xl font-semibold text-white flex items-center">
                    <svg className="w-6 h-6 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" 
                        d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                    Exporter en PDF
                  </h3>
                </div>
                
                <div className="p-6">
                  <InventoryPDF 
                    planId={planId}
                    zones={zones.map(zone => ({
                      ...zone,
                      zoneProduits: planproducts
                        .filter(product => product.zones.some(z => z.id === zone.id))
                        .map(product => {
                          const scanDetail = scanCheckups
                            .flatMap(s => s.details)
                            .find(d => d.produit?.id === product.id);
                          const manualDetail = manualCheckups
                            .flatMap(m => m.details)
                            .find(d => d.produit?.id === product.id);

                          return {
                            ...product,
                            quantiteManuelle: manualDetail?.scannedQuantity,
                            quantiteScan: scanDetail?.scannedQuantity,
                            quantiteTheorique: product.zoneQuantities[zone.id] || 0
                          };
                        })
                    }))}
                  />
                </div>
                
                <div className="flex justify-end gap-3 p-6 bg-gray-50">
                  <button
                    onClick={() => setShowPDF(false)}
                    className="px-5 py-2.5 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-100 transition"
                  >
                    Retour
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Inventory;