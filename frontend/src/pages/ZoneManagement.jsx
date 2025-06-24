import React, { useState, useEffect } from 'react';
import Sidebarsuper from '../components/Sidebar';
import {
  Typography,
  Button,
  Modal,
  ModalDialog,
  Input,
  FormControl,
  FormLabel,
  Alert,
  Textarea
} from '@mui/joy';
import { Edit, Delete, Add, LocationOn } from '@mui/icons-material';

const ZoneManagement = () => {
  const [zones, setZones] = useState([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingZone, setEditingZone] = useState(null);
  const [formData, setFormData] = useState({ name: '', description: '' });
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const [productsModalOpen, setProductsModalOpen] = useState(false);
  const [selectedZone, setSelectedZone] = useState(null);
  const [availableProducts, setAvailableProducts] = useState([]);
  const [selectedProducts, setSelectedProducts] = useState([]);
  const [productQuantities, setProductQuantities] = useState([]);
  const [zoneProducts, setZoneProducts] = useState([]);
  const [remainingQty, setRemainingQty] = useState({});

  const token = localStorage.getItem('token');
  const authHeaders = token ? { 'Authorization': `Bearer ${token}` } : {};

  const fetchZones = async () => {
    try {
      const response = await fetch('http://localhost:8080/Zone/all');
      if (!response.ok) throw new Error('Failed to fetch zones');
      const data = await response.json();
      console.log('Zones data:', data);
      setZones(data);
    } catch (error) {
      console.error('Error fetching zones:', error);
      setError(error.message);
    }
  };

  const fetchAvailableProducts = async () => {
    try {
      const response = await fetch('http://localhost:8080/produits');
      if (!response.ok) throw new Error('Failed to fetch products');
      const data = await response.json();
      setAvailableProducts(data);
    } catch (error) {
      setError(error.message);
    }
  };

  const fetchZoneProducts = async (zoneId) => {
    try {
      const response = await fetch(`http://localhost:8080/Zones/${zoneId}/products`, {
        method: 'GET',
        headers: authHeaders
      });
      if (!response.ok) throw new Error('Failed to fetch zone products');
      const data = await response.json();
      setZoneProducts(data);
      return data; 
    } catch (error) {
      console.error('Error fetching zone products:', error);
      setError(error.message);
      return [];
    }
  };

  useEffect(() => {
    fetchZones();
    fetchAvailableProducts();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const zoneData = {
        name: formData.name,
        description: formData.description,
      };

      const url = editingZone 
        ? `http://localhost:8080/Zone/update/${editingZone.id}`
        : 'http://localhost:8080/Zone';
      
      const response = await fetch(url, {
        method: editingZone ? 'PUT' : 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...authHeaders
        },
        body: JSON.stringify(zoneData),
      });

      if (!response.ok) {
        const errorData = await response.text();
        throw new Error(`Failed to save zone: ${errorData}`);
      }
      
      fetchZones();
      setModalOpen(false);
      setFormData({ name: '', description: '' });
      setEditingZone(null);
    } catch (error) {
      console.error('Error saving zone:', error);
      setError(error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (zoneId) => {
    if (!window.confirm('Êtes-vous sûr de vouloir supprimer cette zone ?')) return;
    
    try {
      const response = await fetch(`http://localhost:8080/Zones/${zoneId}`, {
        method: 'DELETE',
        headers: authHeaders
      });

      if (!response.ok) throw new Error('Failed to delete zone');
      
      fetchZones();
    } catch (error) {
      setError(error.message);
    }
  };

  const handleEdit = (zone) => {
    setEditingZone(zone);
    setFormData({
      name: zone.name,
      description: zone.description
    });
    setModalOpen(true);
  };

const handleManageProducts = async (zone) => {
  setSelectedZone(zone);
  setError(null);
  setProductQuantities({});
  setSelectedProducts([]);
  setRemainingQty({});
  setZoneProducts([]);
  
  const quantities = {};
  zone.zoneProduits?.forEach(zp => {
    const prodId = zp.id.produitId;
    quantities[prodId] = zp.quantiteTheorique || 0;
  });
  setProductQuantities(quantities);

  // Produits sélectionnés pour cette zone
  const selected = availableProducts.filter(prod =>
    zone.zoneProduits?.some(zp => zp.id.produitId === prod.id)
  );
  setSelectedProducts(selected);

  // Recalculer le stock disponible par produit (quantité totale - autres zones)
  const remaining = {};
  availableProducts.forEach(prod => {
    const total = prod.quantitetheo || 0;
    let allocatedElsewhere = 0;

    zones.forEach(z => {
      if (z.id === zone.id) return; // skip current zone
      z.zoneProduits?.forEach(zp => {
        if (zp.id.produitId === prod.id) {
          allocatedElsewhere += zp.quantiteTheorique || 0;
        }
      });
    });

    remaining[prod.id] = total - allocatedElsewhere;
  });
  setRemainingQty(remaining);

  setProductsModalOpen(true);
};



  const handleToggleProduct = (product) => {
    setSelectedProducts(prev => {
      const exists = prev.find(p => p.id === product.id);
      if (exists) {
        return prev.filter(p => p.id !== product.id);
      } else {
        return [...prev, product];
      }
    });
  };

  const handleSaveProducts = async () => {
  setLoading(true);
  try {
    const zoneData = {
      id: selectedZone.id,
      name: selectedZone.name,
      description: selectedZone.description,
      zoneProduits: selectedProducts.map(product => ({
        produitId: product.id,
        quantitetheo: productQuantities[product.id] || 0,
      })),
    };

    const response = await fetch(`http://localhost:8080/Zone/update/${selectedZone.id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        ...authHeaders,
      },
      body: JSON.stringify(zoneData),
    });

    if (!response.ok) {
      const errorData = await response.text();
      throw new Error(`Failed to save zone products: ${errorData}`);
    }

    const updatedZoneProducts = await fetchZoneProducts(selectedZone.id);
    setZoneProducts(updatedZoneProducts);

    await fetchZones();
    const updatedQuantities = {};
updatedZoneProducts.forEach(prod => {
  updatedQuantities[prod.id] = prod.quantitetheo || 0;
});
setProductQuantities(updatedQuantities);

const newRemaining = computeRemaining(updatedQuantities, selectedZone.id);
setRemainingQty(newRemaining);
    setProductsModalOpen(false);
  } catch (error) {
    console.error('Error saving zone products:', error);
    setError(error.message);
  } finally {
    setLoading(false);
  }
};

  const computeRemaining = (currentQuantities, zoneId) => {
  const rem = {};
  availableProducts.forEach(prod => {
    const total = prod.quantitetheo || 0;
    let allocated = 0;

    zones.forEach(z => {
      // Si autre zone => somme normale
      if (z.id !== zoneId) {
        z.zoneProduits?.forEach(zp => {
          if (zp.id.produitId === prod.id) {
            allocated += zp.quantiteTheorique || 0;
          }
        });
      } else {
        // Pour la zone actuelle : utiliser `currentQuantities`
        allocated += currentQuantities[prod.id] || 0;
      }
    });

    rem[prod.id] = Math.max(total - allocated, 0);
  });

  return rem;
};


  return (
       <div className="flex min-h-screen bg-gradient-to-br from-blue-50 to-indigo-50">
      <Sidebarsuper />
      <div className="flex-1 p-6">
        <div className="bg-white rounded-2xl shadow-xl p-6 max-w-6xl mx-auto">
          {/* Header */}
          <div className="flex flex-col md:flex-row justify-between items-center mb-6">
            <div>
              <h1 className="text-2xl md:text-3xl font-bold text-gray-800 flex items-center">
                <LocationOn className="w-8 h-8 mr-3 text-blue-600" />
                Gestion des Zones
              </h1>
              <p className="text-gray-600 mt-2">
                Créez et gérez les zones de votre inventaire
              </p>
            </div>
            <Button
              onClick={() => {
                setEditingZone(null);
                setFormData({ name: '', description: '' });
                setModalOpen(true);
              }}
              className="mt-4 md:mt-0 bg-gradient-to-r from-blue-600 to-indigo-700 text-white"
              startDecorator={<Add />}
            >
              Nouvelle Zone
            </Button>
          </div>

          {/* Zones Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {zones.map((zone) => (
              <div
                key={zone.id}
                className="bg-white rounded-xl border border-gray-200 shadow-sm hover:shadow-md transition p-4"
              >
                <div className="flex justify-between items-start mb-3">
                  <div>
                    <h3 className="text-lg font-semibold text-gray-800">
                      {zone.name}
                    </h3>
                    <p className="text-gray-600 text-sm mt-1">
                      {zone.description || 'Aucune description'}
                    </p>
                  </div>
                  <div className="flex gap-2">
                    <button
                      onClick={() => handleManageProducts(zone)}
                      className="p-2 text-green-600 hover:bg-green-50 rounded-full transition"
                    >
                      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                      </svg>
                    </button>
                    <button
                      onClick={() => handleEdit(zone)}
                      className="p-2 text-blue-600 hover:bg-blue-50 rounded-full transition"
                    >
                      <Edit className="w-5 h-5" />
                    </button>
                    <button
                      onClick={() => handleDelete(zone.id)}
                      className="p-2 text-red-600 hover:bg-red-50 rounded-full transition"
                    >
                      <Delete className="w-5 h-5" />
                    </button>
                  </div>
                </div>
                {/* Update the card's product count display */}
                <div className="flex gap-2 text-sm text-gray-500">
                  <span>{zone.zoneProduits?.length || 0} produits</span>
                  <span>•</span>
                  <span>{zone.agents?.length || 0} agents</span>
                </div>
              </div>
            ))}
          </div>

          {/* Add/Edit Zone Modal */}
          <Modal open={modalOpen} onClose={() => setModalOpen(false)}>
            <ModalDialog className="bg-white rounded-2xl shadow-2xl max-w-md w-full mx-4">
              <div className="bg-gradient-to-r from-blue-600 to-indigo-700 -m-3 mb-4 p-4 rounded-t-2xl">
                <Typography level="h4" className="text-white">
                  {editingZone ? 'Modifier la Zone' : 'Nouvelle Zone'}
                </Typography>
              </div>

              <form onSubmit={handleSubmit} className="space-y-4">
                <FormControl>
                  <FormLabel>Nom de la zone</FormLabel>
                  <Input
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    required
                  />
                </FormControl>

                <FormControl>
                  <FormLabel>Description</FormLabel>
                  <Textarea
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    minRows={3}
                  />
                </FormControl>

                {error && (
                  <Alert color="danger" variant="soft" className="my-2">
                    {error}
                  </Alert>
                )}

                <div className="flex justify-end gap-3">
                  <Button
                    variant="soft"
                    color="neutral"
                    onClick={() => setModalOpen(false)}
                  >
                    Annuler
                  </Button>
                  <Button
                    type="submit"
                    loading={loading}
                    className="bg-gradient-to-r from-blue-600 to-indigo-700 text-white"
                  >
                    {editingZone ? 'Modifier' : 'Créer'}
                  </Button>
                </div>
              </form>
            </ModalDialog>
          </Modal>
          <Modal open={productsModalOpen} onClose={() => setProductsModalOpen(false)}>
            <ModalDialog className="bg-white rounded-2xl shadow-2xl max-w-4xl w-full mx-4">
              <div className="bg-gradient-to-r from-blue-600 to-indigo-700 -m-3 mb-4 p-4 rounded-t-2xl">
                <Typography level="h4" className="text-white">
                  Gérer les produits - {selectedZone?.name}
                </Typography>
              </div>

              <div className="max-h-[60vh] overflow-y-auto">
                <table className="w-full">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-4 py-2 text-left">Produit</th>
                      <th className="px-4 py-2 text-left">Catégorie</th>
                      <th className="px-4 py-2 text-left">Quantité</th>
                      <th className="px-4 py-2 text-left">Disponible</th>
                      <th className="px-4 py-2 text-center">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {availableProducts.map((product) => {
                      const zoneProduct = zoneProducts.find(zp => zp.id === product.id);
                      const isSelected = selectedProducts.find(p => p.id === product.id);
                      
                      return (
                        <tr key={product.id} className="border-t">
                          <td className="px-4 py-2">{product.nom}</td>
                          <td className="px-4 py-2">{product.category?.name}</td>
                         <td className="px-4 py-2">
 <Input
  type="number"
  value={productQuantities[product.id] ?? 0}
  onChange={(e) => {
    const newQty = parseInt(e.target.value) || 0;

    const oldQty = productQuantities[product.id] ?? 0;
    const maxAllowed = (remainingQty[product.id] ?? 0) + oldQty;
    const cappedQty = Math.min(newQty, maxAllowed);

    const newQuantities = {
      ...productQuantities,
      [product.id]: cappedQty,
    };
    setProductQuantities(newQuantities);
    setRemainingQty(computeRemaining(newQuantities, selectedZone.id));
  }}
  min="0"
  className="w-24"
/>

</td>
                          <td className="px-4 py-2 text-gray-500">
                            {remainingQty[product.id] ?? '-'}
                          </td>
                          <td className="px-4 py-2 text-center">
                            <Button
                              size="sm"
                              variant={isSelected ? "soft" : "solid"}
                              color={isSelected ? "danger" : "primary"}
                              onClick={() => handleToggleProduct(product)}
                            >
                              {isSelected ? "Retirer" : "Ajouter"}
                            </Button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>

              <div className="flex justify-end gap-3 mt-4">
                <Button
                  variant="soft"
                  color="neutral"
                  onClick={() => setProductsModalOpen(false)}
                >
                  Annuler
                </Button>
                <Button
                  onClick={handleSaveProducts}
                  loading={loading}
                  className="bg-gradient-to-r from-blue-600 to-indigo-700 text-white"
                >
                  Enregistrer
                </Button>
              </div>
            </ModalDialog>
          </Modal>
        </div>
      </div>
    </div>
  )
};

export default ZoneManagement;