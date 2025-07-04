import React, { useEffect, useState, useRef } from 'react';
import Sidebarsuper from '../components/Sidebar';
import axios from 'axios';
import { useTheme, useMediaQuery } from '@mui/material';
import { toast, ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import InventoryPDF from '../components/InventoryPDF';
import { getUserIdFromToken, getRoleFromToken } from '../utils/auth';
import { exportProductsToExcel } from '../services/exportExcel';

const ITEMS_PER_PAGE = 5;

const Inventory = () => {
  const [plans, setPlans] = useState([]);
  const [manualCheckups, setManualCheckups] = useState([]);
  const [scanCheckups, setScanCheckups] = useState([]);
  const [loading, setLoading] = useState(true);
  const [planId, setPlanId] = useState(() => {
    const stored = localStorage.getItem('selectedPlanId');
    return stored ? Number(stored) : null;
  });
  const userId = getUserIdFromToken();
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
  const [selectedProduct, setSelectedProduct] = useState(null);
  const firstLoadRef = useRef(true);
  const userRole = getRoleFromToken();
  const [manualQuantities, setManualQuantities] = useState({});

  const [validatedProducts, setValidatedProducts] = useState([]);

  useEffect(() => {
    const fetchPlans = async () => {
      try {
        const baseUrl = 'http://localhost:8080/api/plans';
        console.log('Current userId for plan filtering:', userId);
        const url = baseUrl;
        const response = await axios.get(url);
        let plans = Array.isArray(response.data) ? response.data : [response.data];

        if (userRole === 'ADMIN_CLIENT') {
          plans = plans.filter(plan => plan.createur?.id === userId);
        } else if (userRole === 'AGENT_INVENTAIRE') {
          plans = plans.filter(plan =>
            plan.assignations && Array.isArray(plan.assignations) &&
            plan.assignations.some(a => a.agent && a.agent.id === userId)
          );
        }

        setAvailablePlans(prevPlans => {
          const newData = JSON.stringify(plans);
          const oldData = JSON.stringify(prevPlans);
          return newData !== oldData ? plans : prevPlans;
        });
      } catch (error) {
        console.error('Error fetching plans:', error);
        setAvailablePlans([]);
        setError('Failed to load plans');
      } finally {
        if (firstLoadRef.current) {
          setLoading(false);
        }
      }
    };
    fetchPlans();
  }, []);

  useEffect(() => {
    if (planId == null && availablePlans.length > 0) {
      const firstId = availablePlans[0].id;
      setPlanId(firstId);
      localStorage.setItem('selectedPlanId', firstId);
    } else if (planId != null && availablePlans.length > 0) {
      silentUpdate();
    }
  }, [availablePlans, planId]);

  useEffect(() => {
    if (planId == null) return;

    const fetchZonesAndPlan = async () => {
      if (firstLoadRef.current) {
        setLoading(true);
      }
      try {
        const zonesRes = await axios.get('http://localhost:8080/Zone/all');
        const allZones = zonesRes.data || [];
        const planRes = await axios.get(`http://localhost:8080/api/plans/${planId}/details`);
        const zoneProductsRes = await axios.get(`http://localhost:8080/api/plans/${planId}/zone-products`);
        const validatedRes = await axios.get(`http://localhost:8080/api/plans/${planId}/validated-products`);
        const validated = validatedRes.data || [];
        setValidatedProducts(validated);
        const zones = allZones;
        const zoneProducts = zoneProductsRes.data || {};
        const productsWithZones = Object.entries(zoneProducts).reduce((acc, [zoneId, products = []]) => {
          products.forEach(product => {
            const existingProduct = acc.find(p => p.id === product.id);
            const zoneObj = zones.find(z => z.id === Number(zoneId));
            const zoneWithProducts = zones.find(z => z.id === Number(zoneId));
            const zoneProduit = zoneWithProducts?.zoneProduits?.find(zp => zp.id.produitId === product.id && zp.id.zoneId === Number(zoneId));
            const quantiteTheo = zoneProduit?.quantiteTheorique || product.quantiteTheorique || 0;
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
        const filtered = productsWithZones.flatMap(p => {
          const validatedForProduct = validated.filter(vp => vp.id === p.id);
          if (validatedForProduct.length === 0) return [p];
          const remainingZones = p.zones.filter(z => !validatedForProduct.some(vp => Number(vp.validatedZone) === z.id));
          if (remainingZones.length === 0) return [];
          const updatedZoneQuantities = { ...p.zoneQuantities };
          validatedForProduct.forEach(vp => { delete updatedZoneQuantities[vp.validatedZone]; });
          return [{ ...p, zones: remainingZones, zoneQuantities: updatedZoneQuantities }];
        });
        setPlanProducts(filtered);
        setZones(zones);
        const allCheckupsRes = await axios.get(`http://localhost:8080/checkups/plan/${planId}/type/ALL`);
        const allCheckups = allCheckupsRes.data || [];
        const manualCheckups = allCheckups.map(checkup => ({
          ...checkup,
          details: checkup.details.filter(d => d.manualQuantity !== null || d.manualQuantity !== undefined)
        }));
        const scanCheckups = allCheckups.map(checkup => ({
          ...checkup,
          details: checkup.details.filter(d => d.scannedQuantity !== null || d.scannedQuantity !== undefined)
        }));
        setManualCheckups(manualCheckups);
        setScanCheckups(scanCheckups);
      } catch (error) {
        console.error('Error fetching data:', error);
        setError(error.message);
      } finally {
        if (firstLoadRef.current) {
          setLoading(false);
          firstLoadRef.current = false;
        }
      }
    };
    fetchZonesAndPlan();
  }, [planId]);

  const refreshCheckups = async () => {
    try {
      const allCheckupsRes = await axios.get(`http://localhost:8080/checkups/plan/${planId}/type/ALL`);
      const allCheckups = allCheckupsRes.data || [];
      const manualCheckups = allCheckups.map(checkup => ({
        ...checkup,
        details: checkup.details.filter(d => d.manualQuantity !== null || d.manualQuantity !== undefined)
      }));
      const scanCheckups = allCheckups.map(checkup => ({
        ...checkup,
        details: checkup.details.filter(d => d.scannedQuantity !== null || d.scannedQuantity !== undefined)
      }));
      setManualCheckups(manualCheckups);
      setScanCheckups(scanCheckups);
    } catch (error) {
      console.error('Error refreshing checkups:', error);
      toast.error("Erreur lors du rechargement des contrôles");
    }
  };

  const handleRaccomptage = async (checkupId, produitId, zoneId) => {
    if (!checkupId) {
      toast.warning("Recomptage non disponible pour ce produit (aucun contrôle manuel trouvé)");
      return;
    }
    setSelectedCheckupId(checkupId);
    setSelectedProduct({ id: produitId, zoneId: zoneId });
    setShowJustificationModal(true);
  };

  const submitRecomptage = async () => {
    if (!justification.trim()) {
      toast.error("Veuillez fournir une justification");
      return;
    }

    if (!selectedCheckupId || !selectedProduct) {
      toast.error("Données de recomptage incomplètes");
      return;
    }

    try {
      toast.info('Traitement de la demande de recomptage...', {
        toastId: 'recomptage-start'
      });

      await axios.patch(
        `http://localhost:8080/checkups/${selectedCheckupId}/reset-quantities`,
        null,
        {
          params: {
            produitId: selectedProduct.id,
            zoneId: selectedProduct.zoneId
          }
        }
      );

      const response = await axios.put(
        `http://localhost:8080/checkups/${selectedCheckupId}/recomptage`,
        {
          justification: justification,
          demandeRecomptage: true
        },
        {
          params: {
            produitId: selectedProduct.id,
            zoneId: selectedProduct.zoneId
          }
        }
      );

      if (response.data && response.data.message) {
        toast.success(response.data.message, {
          toastId: 'recomptage-success'
        });
      }

      await silentUpdate(); // Silent update après recomptage
      await refreshCheckups();

      setShowJustificationModal(false);
      setJustification('');
      setSelectedCheckupId(null);
      setSelectedProduct(null);

    } catch (error) {
      console.error('Error during recount process:', error);
      const errorMessage = error.response?.data?.error || error.response?.data || error.message;
      toast.error(`Erreur lors de la demande de recomptage: ${errorMessage}`, {
        toastId: 'recomptage-error'
      });
    }
  };
  const handleValider = async (checkupId, produitId, scannedQty, manualQty, theoreticalQty, zoneIdOverride) => {
    const scanned = Number(scannedQty);
    const manual = Number(manualQty);
    const theoretical = Number(theoreticalQty);
    const currentZone = zoneIdOverride || selectedZone || (planproducts.find(p => p.id === produitId)?.zones[0]?.id);

    if (!currentZone) {
      toast.error('Zone non trouvée');
      return;
    }
    
    const validateProduct = async (finalQty) => {
      try {
        toast.info('Validation en cours...', { toastId: 'validating' });
        await axios.put(`http://localhost:8080/api/plans/${planId}/produits/${produitId}/zones/${currentZone}/valider`, {
          quantiteValidee: finalQty
        });

        if (checkupId) {
          await axios.put(`http://localhost:8080/checkups/${checkupId}/valider`);
        }

        toast.success('Produit validé', { toastId: 'checkup-validated' });

        let url = `http://localhost:8080/api/plans/${planId}/validated-products`;
        if (selectedZone) {
          url += `?zoneId=${selectedZone}`;
        }
        const res = await axios.get(url);
        setValidatedProducts(res.data || []);
        await silentUpdate();
      } catch (error) {
        console.error('Error validating product:', error);
        toast.error('Erreur lors de la validation');
      }
    };

    if (scanned !== manual) {
      if (isNaN(manual)) {
        toast.error('La quantité manuelle n\'est pas un nombre valide');
        return;
      }
      const choice = window.prompt(
        `Les quantités scannées (${scanned}) et manuelles (${manual}) ne correspondent pas.\n` +
        `Veuillez saisir la quantité à valider (scannée: ${scanned}, manuelle: ${manual}):`,
        manual
      );
      let finalQty;
      if (choice === null) {
        toast.info('Validation annulée. Aucune action effectuée.');
        return;
      } else {
        finalQty = Number(choice);
        if (isNaN(finalQty)) {
          toast.error('Quantité choisie invalide');
          return;
        }
        await validateProduct(finalQty);
      }
    } else {
      if (!window.confirm('Confirmer la validation de ce produit ?')) return;
      await validateProduct(manual);
    }
  };
const getQuantityColor = (scannedQty, manualQty, theoreticalQty) => {
  const scanned = Number(scannedQty);
  const manual = Number(manualQty);
  const theoretical = Number(theoreticalQty);
  const red = 'bg-red-100 text-red-800 border-red-200';
  const green = 'bg-green-100 text-green-800 border-green-200';
  const yellow = 'bg-yellow-100 text-yellow-800 border-yellow-200';
  const defaultColor = '';
  let ecartbg = defaultColor;
  let theoreticalbg = defaultColor;
  if ( manual < theoretical) {
    ecartbg = red;
  } else if (scanned > theoretical || manual > theoretical) {
    ecartbg = green;
  }
  if (scanned === manual && manual === theoretical) {
    return {
      scanbg: green,
      manualbg: green,
      theoreticalbg: green,
      ecartbg,
    };
  }
  if (scanned !== manual && manual === theoretical) {
    return {
      scanbg: yellow,
      manualbg: green,
      theoreticalbg: green,
      ecartbg,
    };
  }
  if (manual !== scanned && scanned === theoretical) {
    return {
      scanbg: green,
      manualbg: yellow,
      theoreticalbg: green,
      ecartbg,
    };
  }
  if (theoretical !== scanned && scanned === manual) {
    return {
      scanbg: green,
      manualbg: green,
      theoreticalbg: red,
      ecartbg,
    };
  }
  return {
    scanbg: red,
    manualbg: red,
    theoreticalbg,
    ecartbg,
  };
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

  const exportInventoryExcel = (planId) => {
    const url = `http://localhost:8080/api/plans/${planId}/ecarts/export/xlsx`;
    window.open(url, '_blank');
  };

  useEffect(() => {
    const initialLoad = async () => {
      if (!planId) return;
      try {
        setLoading(true);
        const productsRes = await axios.get(`http://localhost:8080/api/plans/${planId}/produits`);
        setPlanProducts(Array.isArray(productsRes.data) ? productsRes.data : []);
      } catch (error) {
        console.error('Error fetching inventory data:', error);
        toast.error('Erreur lors du chargement des données');
        setPlanProducts([]);
      } finally {
        setLoading(false);
      }
    };
    initialLoad();
  }, [planId]);

  const refreshData = async () => {
    await silentUpdate();
  };

  const silentUpdate = async () => {
    try {
      const productsRes = await axios.get(`http://localhost:8080/api/plans/${planId}/produits`);
      setPlanProducts(Array.isArray(productsRes.data) ? productsRes.data : []);
    } catch (error) {
      console.error('Error updating data:', error);
      setPlanProducts([]);
    }
  };

  const getZoneQuantity = (zoneId, product) => {
    const zone = zones.find(z => z.id === Number(zoneId));
    const zoneProduit = zone?.zoneProduits?.find(
      (zp) => zp.id.produitId === product.id && zp.id.zoneId === Number(zoneId)
    );
    return zoneProduit?.quantiteTheorique || 0;
  };

  function buildCheckupDetails(products, type) {
    return products.map(product => {
      const detail = { produit: { id: product.id }, type };
      if (type === 'MANUEL') {
        if (manualQuantities[product.id] !== undefined && manualQuantities[product.id] !== "") {
          detail.manualQuantity = Number(manualQuantities[product.id]);
        }
      } else if (type === 'SCAN') {
        if (product.scannedQuantity !== undefined) {
          detail.scannedQuantity = product.scannedQuantity;
        }
      }
      return detail;
    });
  }

  useEffect(() => {
    if (!planId) return;
    const fetchValidatedProducts = async () => {
      try {
        let url = `http://localhost:8080/api/plans/${planId}/validated-products`;
        if (selectedZone) {
          url += `?zoneId=${selectedZone}`;
        }
        const res = await axios.get(url);
        setValidatedProducts(res.data || []);
      } catch (err) {
        setValidatedProducts([]);
      }
    };
    fetchValidatedProducts();
  }, [planId, selectedZone]);

  const handleScan = async (checkupId, produitId, zoneId, quantity = 1) => {
    try {
      await axios.patch(
        `http://localhost:8080/checkups/scan/${checkupId}`,
        null,
        {
          params: {
            produitId,
            zoneId,
            quantity
          }
        }
      );

      await refreshCheckups();
      await refreshData();

      toast.success('Scan enregistré avec succès');
    } catch (error) {
      console.error('Erreur lors du scan:', error);
      toast.error('Erreur lors du scan: ' + (error.response?.data?.error || error.message));
    }
  };

  const handleBarcodeScan = async (barcode) => {
    try {
      const product = planproducts.find(p => p.codeBarre === barcode);
      if (!product) {
        toast.error('Produit non trouvé pour ce code-barre');
        return;
      }

      const relevantCheckup = scanCheckups.find(checkup =>
        checkup.details.some(detail =>
          detail.produit.id === product.id &&
          detail.zone.id === selectedZone.id
        )
      );

      if (!relevantCheckup) {
        toast.error('Aucun contrôle trouvé pour ce produit dans cette zone');
        return;
      }

      await handleScan(
        relevantCheckup.id,
        product.id,
        selectedZone.id
      );

    } catch (error) {
      console.error('Erreur lors du traitement du scan:', error);
      toast.error('Erreur lors du traitement du scan');
    }
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

      <div className="flex-1 min-w-0 overflow-x-hidden">
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
                  onClick={() => exportProductsToExcel(planproducts, `inventaire_plan_${planId}.xlsx`, validatedProducts)}
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
                      value={planId ?? ''}
                      onChange={(e) => {
                        const id = Number(e.target.value);
                        setPlanId(id);
                        localStorage.setItem('selectedPlanId', id);
                      }}
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
                  className={`py-2 px-4 font-medium text-sm border-b-2 -mb-px transition ${activeTab === 'products'
                      ? 'border-blue-600 text-blue-600'
                      : 'border-transparent text-gray-500 hover:text-gray-700'
                    }`}
                  onClick={() => setActiveTab('products')}
                >
                  Vue par produit
                </button>
                <button
                  className={`py-2 px-4 font-medium text-sm border-b-2 -mb-px transition ${activeTab === 'zones'
                      ? 'border-blue-600 text-blue-600'
                      : 'border-transparent text-gray-500 hover:text-gray-700'
                    }`}
                  onClick={() => setActiveTab('zones')}
                >
                  Vue par zone
                </button>
                <button
                  className={`py-2 px-4 font-medium text-sm border-b-2 -mb-px transition ${activeTab === 'validated'
                      ? 'border-green-600 text-green-600'
                      : 'border-transparent text-gray-500 hover:text-gray-700'
                    }`}
                  onClick={() => setActiveTab('validated')}
                >
                  Produits validés
                </button>
              </div>
            </div>

            {activeTab === 'products' ? (
              <div className="overflow-hidden rounded-xl border border-gray-200 shadow-sm">
                <table className="w-full">
                  <thead className="bg-gradient-to-r from-blue-600 to-indigo-700 text-white"><tr>
                    <th className="p-4 text-left">Produit</th>
                    <th className="p-4 text-left">Référence</th>
                    <th className="p-4 text-left">Zone</th>
                    <th className="p-4 text-right">Qté Théorique</th>
                    <th className="p-4 text-right">Manuel</th>
                    <th className="p-4 text-right">Scan</th>
                    <th className="p-4 text-center">Écart</th>
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


                        let zoneIds = product.zones.map(z => z.id);
                        let usedZoneId = selectedZone ? Number(selectedZone) : (zoneIds.length === 1 ? zoneIds[0] : null);

                        let manual = "-";
                        let scanned = "-";
                        if (usedZoneId) {
                          const detail = manualCheckups.concat(scanCheckups).flatMap(c => c.details)
                            .find(d => d.produit?.id === product.id && d.zone?.id === usedZoneId);
                          if (detail) {
                            manual = detail.manualQuantity !== undefined && detail.manualQuantity !== null ? Number(detail.manualQuantity) : "-";
                            scanned = detail.scannedQuantity !== undefined && detail.scannedQuantity !== null ? Number(detail.scannedQuantity) : "-";
                          }
                        } else {
                          const details = manualCheckups.concat(scanCheckups).flatMap(c => c.details)
                            .filter(d => d.produit?.id === product.id);
                          let manualSum = 0;
                          let scannedSum = 0;
                          let foundManual = false;
                          let foundScanned = false;
                          details.forEach(d => {
                            if (d.manualQuantity !== undefined && d.manualQuantity !== null) {
                              manualSum += Number(d.manualQuantity);
                              foundManual = true;
                            }
                            if (d.scannedQuantity !== undefined && d.scannedQuantity !== null) {
                              scannedSum += Number(d.scannedQuantity);
                              foundScanned = true;
                            }
                          });
                          manual = foundManual ? manualSum : "-";
                          scanned = foundScanned ? scannedSum : "-";
                        }

                        const theoreticalQty = selectedZone
                          ? getZoneQuantity(selectedZone, product)
                          : product.zones.reduce((sum, zone) => sum + getZoneQuantity(zone.id, product), 0);
                        const { scanbg, manualbg, theoreticalbg,ecartbg } = getQuantityColor(scanned, manual, theoreticalQty);
                        const zoneNames = product.zones?.map(z => z?.name).filter(Boolean).join(', ') || '-';

                        return (
                          <tr key={product.id} className="border-b hover:bg-blue-50 transition">
                            <td className="p-4 font-medium text-gray-800">{product.nom || '-'}</td>
                            <td className="p-4 text-blue-600 font-mono">{product.codeBarre || '-'}</td>
                            <td className="p-4 text-gray-600">
                              {product.zones && product.zones.length > 0 ?
                                product.zones.map(zone => {
                                  if (!zone) return '-';
                                  const zoneName = zone.nom || zone.name || zone.designation || '-';
                                  return zoneName;
                                }).join(', ')
                                : '-'}
                            </td>
                            <td className={`p-4 text-right font-medium ${theoreticalbg}`}>
                              {theoreticalQty}
                            </td>
                            <td className={`p-4 text-right font-bold ${manualbg}`}>
                              <input
                                type="number"
                                min="0"
                                value={manualQuantities[product.id] !== undefined ? manualQuantities[product.id] : (manual !== '-' ? manual : '')}
                                onBlur={async e => {
                                  const value = e.target.value;
                                  setManualQuantities(prev => ({ ...prev, [product.id]: value }));
                                  let usedZoneId = selectedZone ? Number(selectedZone) : (product.zones.length === 1 ? product.zones[0].id : null);
                                  if (!usedZoneId) return;
                                  const manualDetail = manualCheckups.concat(scanCheckups).flatMap(c => c.details)
                                    .find(d => d.produit?.id === product.id && d.zone?.id === usedZoneId);
                                  if (manualDetail && manualDetail.checkupId) {
                                    try {
                                      await axios.patch(
                                        `http://localhost:8080/checkups/manual/${manualDetail.checkupId}`,
                                        null,
                                        {
                                          params: {
                                            produitId: product.id,
                                            zoneId: usedZoneId,
                                            quantity: Number(value)
                                          }
                                        }
                                      );
                                      await refreshCheckups();
                                    } catch (err) {
                                      toast.error('Erreur lors de la mise à jour de la quantité manuelle');
                                    }
                                  }
                                }}
                                className="w-20 px-2 py-1 border border-gray-300 rounded text-right focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                                placeholder="-"
                                disabled={userRole === 'ADMIN_CLIENT' || userRole === 'SUPER_ADMIN'}
                              />
                            </td>
                            <td className={`p-4 text-right font-bold ${scanbg}`}>
                              {scanned}
                            </td>
                            <td className="p-4 text-center">                              <span className={`px-3 py-1 rounded-full text-sm font-medium ${ecartbg}`}>
                              {manual !== "-" ? manual - theoreticalQty : "-"}
                            </span>
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
            ) : activeTab === 'zones' ? (
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
                                  const detail = manualCheckups.concat(scanCheckups).flatMap(c => c.details)
                                    .find(d => d.produit?.id === product.id && d.zone?.id === zone.id);
                                  let manual = "-";
                                  let scanned = "-";
                                  let checkupId = null;
                                  if (detail) {
                                    manual = detail.manualQuantity !== undefined && detail.manualQuantity !== null ? Number(detail.manualQuantity) : "-";
                                    scanned = detail.scannedQuantity !== undefined && detail.scannedQuantity !== null ? Number(detail.scannedQuantity) : "-";
                                    checkupId = detail.checkupId || null;
                                  }
                                  const zoneProduit = zone.zoneProduits?.find(zp =>
                                    zp.id.produitId === product.id && zp.id.zoneId === zone.id
                                  );
                                  const theoreticalQty = zoneProduit?.quantiteTheorique || 0;
                                  const { scanbg, manualbg, theoreticalbg,ecartbg } = getQuantityColor(scanned, manual, theoreticalQty);

                                  return (
                                    <tr key={product.id} className="border-b hover:bg-blue-50">
                                      <td className="p-3 font-medium text-gray-800">{product.nom}</td>
                                      <td className="p-3 text-blue-600 font-mono">{product.codeBarre}</td>
                                      <td className={`p-3 text-right font-medium ${theoreticalbg}`}>
                                        {theoreticalQty}
                                      </td>
                                      <td className={`p-3 text-right font-bold ${manualbg}`}>
                                        <input
                                          type="number"
                                          min="0"
                                          value={manualQuantities[product.id] !== undefined ? manualQuantities[product.id] : (manual !== '-' ? manual : '')}
                                          onBlur={async e => {
                                            const value = e.target.value;
                                            setManualQuantities(prev => ({ ...prev, [product.id]: value }));
                                            const manualDetail = manualCheckups.concat(scanCheckups).flatMap(c => c.details)
                                              .find(d => d.produit?.id === product.id && d.zone?.id === zone.id);
                                            if (manualDetail && manualDetail.checkupId) {
                                              try {
                                                await axios.patch(
                                                  `http://localhost:8080/checkups/manual/${manualDetail.checkupId}`,
                                                  null,
                                                  {
                                                    params: {
                                                      produitId: product.id,
                                                      zoneId: zone.id,
                                                      quantity: Number(value)
                                                    }
                                                  }
                                                );
                                                await refreshCheckups();
                                              } catch (err) {
                                                toast.error('Erreur lors de la mise à jour de la quantité manuelle');
                                              }
                                            }
                                          }}
                                          className="w-20 px-2 py-1 border border-gray-300 rounded text-right focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                                          placeholder="-"
                                          disabled={userRole === 'ADMIN_CLIENT' || userRole === 'SUPER_ADMIN'}
                                        />
                                      </td>
                                      <td className={`p-3 text-right font-bold ${scanbg}`}>
                                        {scanned}
                                      </td>
                                      <td className="p-3 text-center">
                                        <span className={`px-3 py-1 rounded-full text-sm font-medium ${ecartbg}`}>
                                          {manual !== "-" ? manual - theoreticalQty : "-"}
                                        </span>
                                      </td>
                                      <td className="p-3 text-center">
                                        {product.status !== 'VERIFIE' && (
                                          <div className="flex justify-center gap-2">
                                            <button
                                              onClick={() => {
                                                const canValidate = manual || scanned;
                                                if (!canValidate) {
                                                  toast.warning('Aucune quantité disponible à valider');
                                                  return;
                                                }
                                                handleValider(
                                                  checkupId,
                                                  product.id,
                                                  scanned,
                                                  manual,
                                                  theoreticalQty,
                                                  zone.id
                                                );
                                              }}
                                              disabled={product.status === 'VERIFIE' || (!manual && !scanned)}
                                              className={`flex items-center gap-1 px-3 py-1.5 rounded text-sm font-medium transition ${product.status === 'VERIFIE'
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
                                                  {(manual !== "-" && scanned !== "-") ? 'Valider' : 'Non disponible'}
                                                </>
                                              )}
                                            </button>
                                            <button
                                              onClick={() => {
                                                const manualDetail = manualCheckups.find(checkup =>
                                                  checkup.details.some(detail => detail.zone?.id === zone.id)
                                                );
                                                if (manualDetail) {
                                                  const detail = manualDetail.details.find(d => d.zone?.id === zone.id);
                                                  if (detail && detail.produit) {
                                                    handleRaccomptage(manualDetail.id, detail.produit.id, zone.id);
                                                  }
                                                }
                                              }}
                                              className="flex items-center gap-1 bg-amber-500 hover:bg-amber-600 text-white px-3 py-1.5 rounded text-sm font-medium transition"
                                              disabled={!manualCheckups.some(checkup =>
                                                checkup.details.some(detail => detail.zone?.id === zone.id)
                                              )}
                                              title={!manualCheckups.some(checkup =>
                                                checkup.details.some(detail => detail.zone?.id === zone.id)
                                              ) ? 'Recomptage non disponible pour ce produit' : 'Recompter'}
                                            >
                                              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                                              </svg>
                                              Recompter
                                            </button>
                                          </div>
                                        )}
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
            ) : activeTab === 'validated' ? (
              <div className="overflow-hidden rounded-xl border border-gray-200 shadow-sm">
                <table className="w-full">
                  <thead className="bg-green-600 text-white">
                    <tr>
                      <th className="p-4 text-left">Produit</th>
                      <th className="p-4 text-left">Référence</th>
                      <th className="p-4 text-left">Zone validée</th>
                      <th className="p-4 text-left">Quantité validée</th>
                      <th className="p-4 text-left">Qté avant validation</th>
                      <th className="p-4 text-left">Écart</th>
                    </tr>
                  </thead>
                  <tbody>
                    {validatedProducts.map(vp => {
                      const quantiteValidee = (vp.quantiteValidee ?? vp.quantite);
                      const quantiteAvant = vp.oldQuantiteAvant !== undefined ? vp.oldQuantiteAvant : (vp.quantiteAvantValidation !== undefined ? vp.quantiteAvantValidation : (vp.quantiteAvant !== undefined ? vp.quantiteAvant : (vp.quantiteAvantValidation === 0 ? 0 : undefined)));
                      let ecart = '-';
                      if (quantiteValidee !== undefined && quantiteValidee !== '-' && quantiteAvant !== undefined && quantiteAvant !== '-') {
                        ecart = Number(quantiteValidee) - Number(quantiteAvant);
                      }
                      return (
                        <tr key={`${vp.id}-${vp.zoneId || vp.validatedZone}`}
                          className="border-b hover:bg-green-50">
                          <td className="p-4">{vp.nom || vp.name}</td>
                          <td className="p-4">{vp.codeBarre}</td>
                          <td className="p-4">{zones.find(z => z.id === Number(vp.zoneId || vp.validatedZone))?.name || '-'}</td>
                          <td className="p-4">{quantiteValidee ?? '-'}</td>
                          <td className="p-4">{quantiteAvant !== undefined ? quantiteAvant : '-'}</td>
                          <td className={
                            `p-4 font-bold ` +
                            (ecart !== '-' && !isNaN(ecart)
                              ? (ecart > 0 ? 'text-green-600' : (ecart < 0 ? 'text-red-600' : 'text-gray-800'))
                              : 'text-gray-800')
                          }>
                            {ecart}
                          </td>
                        </tr>
                      );
                    })}
                    {validatedProducts.length === 0 && (
                      <tr><td className="p-4 text-center" colSpan="6">Aucun produit validé</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
            ) : null}

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
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                    Exporter en PDF
                  </h3>
                </div>
                <div className="p-6">
                  <InventoryPDF
                    planId={planId}
                    zones={
                      zones
                        .map(zone => {
                          const validatedZoneProducts = validatedProducts.filter(vp => (vp.zoneId || vp.validatedZone) == zone.id);
                          if (validatedZoneProducts.length === 0) return null;
                          const zoneProduits = validatedZoneProducts.map(vp => {
                            const full = planproducts.find(p => p.id === vp.id);
                            return full ? { ...full, ...vp } : vp;
                          });
                          return {
                            ...zone,
                            zoneProduits
                          };
                        })
                        .filter(Boolean)
                    }
                    validatedProducts={validatedProducts}
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