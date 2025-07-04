import {
  Box,
  Card,
  FormControl,
  FormLabel,
  Select,
  Option,
  Stack,
  Table,
  Sheet,
  Typography,
  CircularProgress,
  Button,
  IconButton,
  Modal,
  ModalDialog,
  ModalClose,
  Divider,
  DialogTitle,
  DialogContent,
  DialogActions,
  Input
} from '@mui/joy';
import { useState, useEffect } from 'react';
import Sidebarsuper from '../components/Sidebar';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import Warning from '@mui/icons-material/Warning';
import DownloadIcon from '@mui/icons-material/Download';
import SearchIcon from '@mui/icons-material/Search';
import { Add } from '@mui/icons-material';
import ProductListPDF from '../components/ProductListPDF';
import axios from 'axios';
import ZoomableImage from '../components/ZoomableImage';

const ListeProduits = () => {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState('');
  const [selectedSubCategory, setSelectedSubCategory] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [productToDelete, setProductToDelete] = useState(null);
  const [deleteError, setDeleteError] = useState(null);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);
  const [editError, setEditError] = useState(null);
const [showPDF, setShowPDF] = useState(false);
  const [validatedProducts, setValidatedProducts] = useState(new Set());
  const [planStatus, setPlanStatus] = useState('');
  const [planId, setPlanId] = useState(1);
  const [searchQuery, setSearchQuery] = useState('');
  const token = localStorage.getItem('token');
  const authHeaders = token ? { 'Authorization': `Bearer ${token}` } : {};
  useEffect(() => {
    const fetchCategories = async () => {
      try {
        const { data } = await axios.get('http://localhost:8080/api/categories');
        setCategories(Array.isArray(data) ? data : []);
      } catch (error) {
        console.error('Error fetching categories:', error);
      }
    };
    fetchCategories();
  }, []);
  const checkAndUpdatePlanStatus = async (planId) => {
    try {
      const response = await fetch(`http://localhost:8080/produits/plan/${planId}`);
      const products = await response.json();
      
      if (products.length === 0) {
        await axios.put(`http://localhost:8080/api/plans/${planId}`, {
          method: 'PUT',
        headers: authHeaders,
          body: JSON.stringify({ statut: 'Termine' })
        });
        setPlanStatus('Terminer');
      }
    } catch (error) {
      console.error('Error checking plan status:', error);
    }
  };
  useEffect(() => {
    const fetchProducts = async () => {
      try {
        setLoading(true);
        let url = 'http://localhost:8080/produits';
        const params = new URLSearchParams();
        if (selectedCategory) params.append('categoryId', selectedCategory);
        if (selectedSubCategory) params.append('subCategoryId', selectedSubCategory);
        if (params.toString()) url += `?${params.toString()}`;

        const response = await fetch(url);
        if (!response.ok) throw new Error('Failed to fetch products');
        const data = await response.json();
        setProducts(Array.isArray(data) ? data : []);
        if (data.length === 0) {
          await checkAndUpdatePlanStatus(planId);
        }
        setError(null);
      } catch (err) {
        console.error('Error:', err);
        setError(err.message);
        setProducts([]);
      } finally {
        setLoading(false);
      }
    };

    fetchProducts();
  }, [selectedCategory, selectedSubCategory]);

  const getSubCategories = () => {
    if (!selectedCategory) return [];
    const category = categories.find(cat => cat.id === selectedCategory);
    return category?.subCategories || [];
  };

  const handleDeleteClick = (product) => {
    setProductToDelete(product);
    setDeleteModalOpen(true);
    setDeleteError(null);
  };

  const handleDeleteConfirm = async () => {
    try {
      const { status } = await axios.delete(
        `http://localhost:8080/produits/${productToDelete.id}`,
        { headers: authHeaders }
      );

      if (status !== 200 && status !== 204) {
        throw new Error('Échec de la suppression du produit');
      }

      setProducts(prev => prev.filter(p => p.id !== productToDelete.id));
      setDeleteModalOpen(false);
      setProductToDelete(null);
    } catch (error) {
      console.error('Delete error:', error);
      setDeleteError(error.message || 'Erreur lors de la suppression');
    }
  };

  const handleEditClick = (product) => {
    setEditingProduct({ ...product });
    setEditModalOpen(true);
    setEditError(null);
  };

  const handleEditChange = (e) => {
    const { name, value } = e.target;
    setEditingProduct(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const silentRefreshProducts = async () => {
    try {
      let url = 'http://localhost:8080/produits';
      const params = new URLSearchParams();
      if (selectedCategory) params.append('categoryId', selectedCategory);
      if (selectedSubCategory) params.append('subCategoryId', selectedSubCategory);
      if (params.toString()) url += `?${params.toString()}`;
      const response = await fetch(url);
      if (!response.ok) throw new Error('Failed to fetch products');
      const data = await response.json();
      setProducts(Array.isArray(data) ? data : []);
    } catch (err) {
    }
  };

  const handleEditSubmit = async () => {
    try {
      let updatedProduct = { ...editingProduct };
      const produitPayload = { ...editingProduct };
      delete produitPayload.imageFile;
      delete produitPayload.imagePreview;
      const response = await axios.put(
        `http://localhost:8080/produits/${editingProduct.id}`,
        produitPayload,
        { headers: authHeaders }
      );
      updatedProduct = response.data;
      if (editingProduct.imageFile) {
        const formData = new FormData();
        formData.append('image', editingProduct.imageFile);
        await axios.put(
          `http://localhost:8080/produits/${editingProduct.id}/image`,
          formData,
          { headers: { ...authHeaders } }
        );
      }
      setProducts(products.map(p =>
        p.id === updatedProduct.id ? updatedProduct : p
      ));
      setEditModalOpen(false);
      setEditingProduct(null);
      setEditError(null);
      silentRefreshProducts();
    } catch (error) {
      console.error('Edit error:', error);
      setEditError(error.response?.data?.message || error.message || 'Erreur lors de la modification');
    }
  };

  const getProductColor = (product) => {
    const manualQty = Number(product.manualQuantity);
    const scannedQty = Number(product.scannedQuantity);
    const theoreticalQty = Number(product.quantitetheo);

    if (validatedProducts.has(product.id)) {
      return 'success.100'; 
    } else if (manualQty === scannedQty) {
      return 'success.50'; 
    }
    return 'background.surface'; 
  };
  return (
    <>
      <div className="flex min-h-screen bg-gradient-to-br from-blue-50 to-indigo-50">
        <Sidebarsuper />
        <div className="flex-1 overflow-hidden">
          <div className="p-4 md:p-6">
            <div className="bg-white rounded-2xl shadow-xl">
              <div className="p-6 border-b border-gray-200">
                <div className="flex flex-col md:flex-row justify-between items-center">
                  <div>
                    <h1 className="text-2xl md:text-3xl font-bold text-gray-800 flex items-center">
                      <svg className="w-8 h-8 mr-3 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                        <path d="M5 3a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2V5a2 2 0 00-2-2H5zM5 11a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2v-2a2 2 0 00-2-2H5zM11 5a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V5zM14 11a1 1 0 011 1v1h1a1 1 0 110 2h-1v1a1 1 0 11-2 0v-1h-1a1 1 0 110-2h1v-1a1 1 0 011-1z" />
                      </svg>
                      Liste des Produits
                    </h1>
                    <p className="text-gray-600 mt-2">
                      Gérez et consultez tous vos produits
                    </p>
                  </div>
                  <div className="mt-4 md:mt-0 flex gap-3">
                   <button
  onClick={() => setShowPDF(true)}
  className="flex items-center gap-2 bg-gradient-to-r from-blue-600 to-indigo-700 hover:from-blue-700 hover:to-indigo-800 text-white px-4 py-2 rounded-lg shadow-md transition"
>
  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
  </svg>
  Export PDF
</button>
{showPDF && (
  <div className="fixed inset-0 bg-white bg-opacity-90 flex items-center justify-center p-4 z-50">
    <div className="bg-white rounded-2xl shadow-2xl w-full max-w-4xl overflow-hidden">
      <div className="bg-gradient-to-r from-blue-600 to-indigo-700 p-5">
        <h3 className="text-xl font-semibold text-white flex items-center gap-2">
          <svg className="w-6 h-6 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          Exporter en PDF
        </h3>
      </div>
      
      <div className="p-6">
        <ProductListPDF products={products} categories={categories} />
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
                    <Button
                      onClick={() => window.location.href = 'produits/ajouter'}
                      className="bg-gradient-to-r from-blue-600 to-indigo-700 text-white"
                      startIcon={<Add />}
                    >
                      Nouveau Produit
                    </Button>
                  </div>
                </div>
              </div>

              <div className="p-4 bg-blue-50 border-b border-blue-100">
                <div className="flex flex-col md:flex-row gap-4">
                  <FormControl className="md:w-64">
                    <FormLabel className="text-gray-700">Rechercher</FormLabel>
                    <Input
                      placeholder="Rechercher un produit..."
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      startDecorator={<SearchIcon className="text-gray-400" />}
                      className="bg-white"
                    />
                  </FormControl>

                  <FormControl className="md:w-48">
                    <FormLabel className="text-gray-700">Catégorie</FormLabel>
                    <Select
                      value={selectedCategory}
                      onChange={(_, newValue) => {
                        setSelectedCategory(newValue);
                        setSelectedSubCategory('');
                      }}
                      className="bg-white"
                    >
                      <Option value="">Toutes</Option>
                      {categories.map((category) => (
                        <Option key={category.id} value={category.id}>
                          {category.name}
                        </Option>
                      ))}
                    </Select>
                  </FormControl>

                  <FormControl className="md:w-48">
                    <FormLabel className="text-gray-700">Sous-catégorie</FormLabel>
                    <Select
                      value={selectedSubCategory}
                      onChange={(_, newValue) => setSelectedSubCategory(newValue)}
                      disabled={!selectedCategory}
                      className="bg-white"
                    >
                      <Option value="">Toutes</Option>
                      {getSubCategories().map((subCategory) => (
                        <Option key={subCategory.id} value={subCategory.id}>
                          {subCategory.name}
                        </Option>
                      ))}
                    </Select>
                  </FormControl>
                </div>
              </div>
              <div className="p-6">
                {loading ? (
                  <div className="flex justify-center items-center h-64">
                    <CircularProgress size="lg" />
                  </div>
                ) : error ? (
                  <div className="text-center py-12">
                    <div className="inline-block p-4 rounded-full bg-red-100 mb-4">
                      <svg className="w-8 h-8 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                      </svg>
                    </div>
                    <Typography level="h6" color="danger">
                      {error}
                    </Typography>
                  </div>
                ) : (
                  <div className="overflow-hidden rounded-xl border border-gray-200">
                    <Table
                      stickyHeader
                      hoverRow
                      className="w-full"
                    >
                      <thead>
                        <tr>
                          <th style={{ width: '10%' }}>Image</th>
                          <th style={{ width: '20%' }}>Nom</th>
                          <th style={{ width: '20%' }}>Description</th>
                          <th style={{ width: '10%' }}>Code Barre</th>
                          <th style={{ width: '10%' }}>Référence</th>
                          <th style={{ width: '10%' }}>Prix</th>
                          <th style={{ width: '10%' }}>Quantité</th>
                          <th style={{ width: '10%' }}>Catégorie</th>
                          <th style={{ width: '10%' }}>Sous-catégorie</th>
                          <th style={{ width: '10%', textAlign: 'center' }}>Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {products
                          .filter(product => {
                            const categoryMatch = !selectedCategory || product.category?.id === selectedCategory;
                            const subCategoryMatch = !selectedSubCategory || product.subCategory?.id === selectedSubCategory;
                            const searchMatch = !searchQuery || 
                              product.nom?.toLowerCase().includes(searchQuery.toLowerCase()) ||
                              product.codeBarre?.toLowerCase().includes(searchQuery.toLowerCase()) ||
                              product.reference?.toLowerCase().includes(searchQuery.toLowerCase());
                            return categoryMatch && subCategoryMatch && searchMatch;
                          })
                          .map((product) => (
                            <tr 
                              key={product.id}
                              sx={{
                                backgroundColor: getProductColor(product),
                                transition: 'background-color 0.3s'
                              }}
                            >
                              <td>
                                {(() => {
                                  let src = '';
                                  if (product.imageData) {
                                    src = `data:image/jpeg;base64,${product.imageData}`;
                                  } else if (product.imageUrl && typeof product.imageUrl === 'string' && product.imageUrl.trim() !== '') {
                                    if (/^data:image\//.test(product.imageUrl)) {
                                      src = product.imageUrl;
                                    } else if (/^([A-Za-z0-9+/=]{100,})$/.test(product.imageUrl)) {
                                      src = `data:image/jpeg;base64,${product.imageUrl}`;
                                    } else if (/^\/image\//.test(product.imageUrl)) {
                                      src = `http://localhost:8080${product.imageUrl}`;
                                    } else if (/^https?:\/\//.test(product.imageUrl)) {
                                      src = product.imageUrl;
                                    } else {
                                      src = '';
                                    }
                                  }
                                  return src ? (
                                    <ZoomableImage
                                      src={src}
                                      alt={product.nom}
                                      style={{ width: 48, height: 48, objectFit: 'cover', borderRadius: 8 }}
                                      onError={e => { e.target.onerror = null; e.target.src = '/placeholder.png'; }}
                                      on
                                    />
                                  ) : (
                                    <div style={{ width: 48, height: 48, background: '#f3f4f6', borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#bdbdbd', fontSize: 24 }}>
                                      <span role="img" aria-label="no image">🖼️</span>
                                    </div>
                                  );
                                })()}
                              </td>
                              <td>{product.nom}</td>
                              <td>{product.description}</td>
                              <td>{product.codeBarre}</td>
                              <td>{product.reference}</td>
                              <td>{product.prix} €</td>
                              <td>{product.quantitetheo}</td>
                              <td>{product.category?.name}</td>
                              <td>{product.subCategory?.name}</td>
                              <td>
                                <Stack direction="row" spacing={1} justifyContent="center">
                                  <IconButton
                                    size="sm"
                                    variant="soft"
                                    color="primary"
                                    onClick={() => handleEditClick(product)}
                                  >
                                    <EditIcon />
                                  </IconButton>
                                  <IconButton
                                    size="sm"
                                    variant="soft"
                                    color="danger"
                                    onClick={() => handleDeleteClick(product)}
                                  >
                                    <DeleteIcon />
                                  </IconButton>
                                </Stack>
                              </td>
                            </tr>
                          ))}
                      </tbody>
                    </Table>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Update Modals styling */}
        <Modal open={deleteModalOpen} onClose={() => setDeleteModalOpen(false)}>
          <ModalDialog className="bg-white rounded-2xl shadow-2xl max-w-md w-full mx-4">
            <div className="bg-gradient-to-r from-red-600 to-red-700 -m-3 mb-4 p-4 rounded-t-2xl">
              <Typography level="h4" className="text-white flex items-center gap-2">
                <Warning />
                Confirmer la suppression
              </Typography>
            </div>
            <DialogContent>
              Êtes-vous sûr de vouloir supprimer le produit{' '}
              <strong>{productToDelete?.nom}</strong> ?
              {deleteError && (
                <Typography color="danger" fontSize="sm" sx={{ mt: 1 }}>
                  {deleteError}
                </Typography>
              )}
            </DialogContent>
            <DialogActions>
              <Button
                variant="plain"
                color="neutral"
                onClick={() => setDeleteModalOpen(false)}
              >
                Annuler
              </Button>
              <Button
                variant="solid"
                color="danger"
                onClick={handleDeleteConfirm}
              >
                Supprimer
              </Button>
            </DialogActions>
          </ModalDialog>
        </Modal>

        <Modal open={editModalOpen} onClose={() => setEditModalOpen(false)}>
          <ModalDialog className="bg-white rounded-2xl shadow-2xl max-w-xl w-full mx-4">
            <div className="bg-gradient-to-r from-blue-600 to-indigo-700 -m-3 mb-4 p-4 rounded-t-2xl">
              <Typography level="h4" className="text-white">
                Modifier le produit
              </Typography>
            </div>
            <DialogContent>
              <Stack spacing={2}>
                <FormControl>
                  <FormLabel>Nom</FormLabel>
                  <Input
                    name="nom"
                    value={editingProduct?.nom || ''}
                    onChange={handleEditChange}
                  />
                </FormControl>
                
                <FormControl>
                  <FormLabel>Description</FormLabel>
                  <Input
                    name="description"
                    value={editingProduct?.description || ''}
                    onChange={handleEditChange}
                  />
                </FormControl>

                <FormControl>
                  <FormLabel>Code Barre</FormLabel>
                  <Input
                    name="codeBarre"
                    value={editingProduct?.codeBarre || ''}
                    onChange={handleEditChange}
                  />
                </FormControl>

                <FormControl>
                  <FormLabel>Référence</FormLabel>
                  <Input
                    name="reference"
                    value={editingProduct?.reference || ''}
                    onChange={handleEditChange}
                  />
                </FormControl>

                <FormControl>
                  <FormLabel>Prix</FormLabel>
                  <Input
                    type="number"
                    name="prix"
                    value={editingProduct?.prix || ''}
                    onChange={handleEditChange}
                  />
                </FormControl>

                <FormControl>
                  <FormLabel>Quantité</FormLabel>
                  <Input
                    type="number"
                    name="quantitetheo"
                    value={editingProduct?.quantitetheo || ''}
                    onChange={handleEditChange}
                  />
                </FormControl>

                <FormControl>
                  <FormLabel>Image</FormLabel>
                  <Input
                    type="file"
                    accept="image/*"
                    onChange={e => {
                      const file = e.target.files[0];
                      setEditingProduct(prev => ({
                        ...prev,
                        imageFile: file,
                        imagePreview: file ? URL.createObjectURL(file) : prev.imagePreview
                      }));
                    }}
                  />
                  {editingProduct?.imageFile ? (
                    <img
                      src={editingProduct.imagePreview}
                      alt="Aperçu"
                      style={{ width: 80, height: 80, objectFit: 'cover', borderRadius: 8, marginTop: 8 }}
                    />
                  ) : editingProduct?.imageUrl || editingProduct?.imageData ? (
                    <img
                      src={editingProduct.imageData ? `data:image/jpeg;base64,${editingProduct.imageData}` : editingProduct.imageUrl}
                      alt="Produit"
                      style={{ width: 80, height: 80, objectFit: 'cover', borderRadius: 8, marginTop: 8 }}
                    />
                  ) : null}
                </FormControl>

                {editError && (
                  <Typography color="danger" fontSize="sm">
                    {editError}
                  </Typography>
                )}
              </Stack>
            </DialogContent>
            <DialogActions>
              <Button
                variant="plain"
                color="neutral"
                onClick={() => setEditModalOpen(false)}
              >
                Annuler
              </Button>
              <Button
                variant="solid"
                color="primary"
                onClick={handleEditSubmit}
              >
                Enregistrer
              </Button>
            </DialogActions>
          </ModalDialog>
        </Modal>
      </div>
    </>
  );
};

export default ListeProduits;