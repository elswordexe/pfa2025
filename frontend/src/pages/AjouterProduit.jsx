import { 
  Card, FormControl, FormLabel, Input, Select, Button, 
  Typography, Stack, Option, CircularProgress, Alert, Box,
  Modal, ModalDialog
} from '@mui/joy';
import { useState, useEffect } from 'react';
import { useFormik } from 'formik';
import * as Yup from 'yup';
import Sidebarsuper from '../components/Sidebar';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import axios from 'axios';

const AjouterProduit = () => {
  const [dragActive, setDragActive] = useState(false);
  const [importStatus, setImportStatus] = useState({ loading: false, error: '', success: '' });
  const [showImportModal, setShowImportModal] = useState(false);
  const [imageFile, setImageFile] = useState(null);
  const [newCategoryName, setNewCategoryName] = useState("");
  const [newSubCategoryName, setNewSubCategoryName] = useState("");
  const [creatingCategory, setCreatingCategory] = useState(false);
  const [creatingSubCategory, setCreatingSubCategory] = useState(false);

  const [categories, setCategories] = useState([]);
  const [sousCategories, setSousCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const token = localStorage.getItem('token');
  const headers = token ? { Authorization: `Bearer ${token}` } : {};

  useEffect(() => {
    const fetchCategories = async () => {
      try {
        setLoading(true);
        const { data } = await axios.get('http://localhost:8080/api/categories');
        
        if (!Array.isArray(data)) {
          console.error('Received data:', data);
          throw new Error('Les données reçues ne sont pas un tableau');
        }
        const validCategories = data.every(category => 
          category && 
          typeof category === 'object' &&
          'id' in category &&
          'name' in category
        );

        if (!validCategories) {
          console.error('Invalid category structure:', data);
          throw new Error('Structure des catégories invalide');
        }

        setCategories(data);
        setError(null);
      } catch (err) {
        console.error('Erreur lors du chargement des catégories:', err);
        setError(err.message);
        setCategories([]);
      } finally {
        setLoading(false);
      }
    };

    fetchCategories();
  }, []);
  const fetchSousCategories = async (categorieId) => {
    if (!categorieId) {
      setSousCategories([]);
      return;
    }

    try {
      const { data } = await axios.get(`http://localhost:8080/api/categories/${categorieId}/sous-categories`);
      setSousCategories(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Error fetching sub-categories:', error);
      setSousCategories([]);
    }
  };

  const handleImageChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      setImageFile(e.target.files[0]);
    }
  };

  const uploadProductImage = async (produitId) => {
    if (!imageFile) return;
    const formData = new FormData();
    formData.append('image', imageFile);
    try {
      await axios.post(`http://localhost:8080/produits/${produitId}/image`, formData, {
        headers: {
          ...headers,
          'Content-Type': 'multipart/form-data',
        },
      });
    } catch (error) {
      console.error('Erreur lors de l\'upload de l\'image:', error);
    }
  };

  const formik = useFormik({
    initialValues: {
      nom: '',
      description: '',
      codeBarre: '',
      reference: '',
      prix: '',
      unite: '',
      quantitetheo: 0,
      categorieId: '',
      sousCategorieId: ''
    },
    validationSchema: Yup.object({
      nom: Yup.string().required('Nom requis'),
      codeBarre: Yup.string().required('Code barre requis'),
      reference: Yup.string().required('Référence requise'),
      prix: Yup.number().typeError('Prix invalide').min(0, 'Prix >= 0').required('Prix requis'),
      quantitetheo: Yup.number().typeError('Quantité invalide').min(0, 'Quantité >= 0').required('Quantité requise'),
      categorieId: Yup.string().required('Catégorie requise'),
    }),
    onSubmit: async (values, { resetForm }) => {
      setLoading(true);
      try {
        const payload = {
          nom: values.nom,
          description: values.description,
          CodeBarre: values.codeBarre,
          reference: values.reference,
          prix: values.prix ? Number(values.prix) : undefined,
          unite: values.unite,
          quantitetheo: Number(values.quantitetheo),
          category: values.categorieId ? { id: values.categorieId } : undefined,
          subCategory: values.sousCategorieId ? { id: values.sousCategorieId } : undefined,
        };
        const { data, status } = await axios.post(
          'http://localhost:8080/produits/register',
          payload,
          { headers }
        );
        if ((status === 200 || status === 201) && data.id) {
          if (imageFile) {
            await uploadProductImage(data.id);
          }
          alert('Produit ajouté avec succès');
          resetForm();
          setImageFile(null);
        }
      } catch (error) {
        console.error('Error adding product:', error);
      } finally {
        setLoading(false);
      }
    },
  });

  const handleFileImport = async (file) => {
    if (!file) return;
    setImportStatus({ loading: true, error: '', success: '' });

    try {
        const fileContent = await file.text();
        let products = JSON.parse(fileContent);

        const response = await axios.post('http://localhost:8080/produits/import/json', products, {
            headers: {
                'Content-Type': 'application/json'
            }
        });

        setImportStatus({
            loading: false,
            success: `${response.data.length} produits importés avec succès`
        });
        setTimeout(() => {
            setShowImportModal(false);
        }, 2000);

    } catch (error) {
        console.error('Import error:', error);
        setImportStatus({
            loading: false,
            error: error.response?.data || 'Erreur lors de l\'import'
        });
    }
  };

  const handleCSVImport = async (file) => {
    if (!file) return;
    setImportStatus({ loading: true, error: '', success: '' });
    const formData = new FormData();
    formData.append('file', file);
    try {
      const response = await axios.post('http://localhost:8080/produits/import/csv', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      setImportStatus({
        loading: false,
        success: `${response.data.count || 0} produits importés avec succès`
      });
      setTimeout(() => setShowImportModal(false), 2000);
    } catch (error) {
      setImportStatus({
        loading: false,
        error: error.response?.data?.message || 'Erreur lors de l\'import CSV'
      });
    }
  };

  const handleExcelImport = async (file) => {
    if (!file) return;
    setImportStatus({ loading: true, error: '', success: '' });
    const formData = new FormData();
    formData.append('file', file);
    try {
      const response = await axios.post('http://localhost:8080/produits/import/excel', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      setImportStatus({
        loading: false,
        success: `${response.data.count || 0} produits importés avec succès`
      });
      setTimeout(() => setShowImportModal(false), 2000);
    } catch (error) {
      setImportStatus({
        loading: false,
        error: error.response?.data?.message || 'Erreur lors de l\'import Excel'
      });
    }
  };

  const handleDrag = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFileImport(e.dataTransfer.files[0]);
    }
  };

  const handleCreateCategory = async () => {
    if (!newCategoryName.trim()) return;
    setCreatingCategory(true);
    try {
      const { data } = await axios.post('http://localhost:8080/api/categories', { name: newCategoryName }, { headers });
      setCategories(prev => [...prev, data]);
      setProduct({ ...product, categorieId: data.id, sousCategorieId: '' });
      setNewCategoryName("");
      fetchSousCategories(data.id);
    } catch (error) {
      alert("Erreur lors de la création de la catégorie");
    } finally {
      setCreatingCategory(false);
    }
  };

  const handleCreateSubCategory = async () => {
    if (!newSubCategoryName.trim() || !product.categorieId) return;
    setCreatingSubCategory(true);
    try {
      const { data } = await axios.post(`http://localhost:8080/api/categories/${product.categorieId}/sous-categories`, { name: newSubCategoryName }, { headers });
      setSousCategories(prev => [...prev, data]);
      setProduct({ ...product, sousCategorieId: data.id });
      setNewSubCategoryName("");
    } catch (error) {
      alert("Erreur lors de la création de la sous-catégorie");
    } finally {
      setCreatingSubCategory(false);
    }
  };

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: '#f5f5f5' }}>
      <Sidebarsuper />
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: { xs: 2, md: 4 },
          bgcolor: 'background.default',
        }}
      >
        <Card
          variant="outlined"
          sx={{
            maxWidth: 800,
            mx: 'auto',
            mt: 2,
            bgcolor: 'white',
            borderRadius: 'xl',
            boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
            overflow: 'hidden',
          }}
        >
          <Box sx={{ p: { xs: 2, md: 4 } }}>
            <Typography level="h4" sx={{ mb: 4, color: 'primary.main' }}>
              Ajouter un Nouveau Produit
            </Typography>

            {loading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
                <CircularProgress size="lg" />
              </Box>
            ) : error ? (
              <Alert color="danger" variant="soft" sx={{ mb: 2 }}>
                {error}
              </Alert>
            ) : (
              <Stack spacing={3}>
                <FormControl required error={formik.touched.nom && Boolean(formik.errors.nom)}>
                  <FormLabel>Nom du produit</FormLabel>
                  <Input
                    name="nom"
                    placeholder="Entrez le nom du produit"
                    variant="outlined"
                    sx={{ bgcolor: 'background.body' }}
                    value={formik.values.nom}
                    onChange={formik.handleChange}
                    onBlur={formik.handleBlur}
                  />
                  {formik.touched.nom && formik.errors.nom && (
                    <Typography color="danger" level="body-sm">{formik.errors.nom}</Typography>
                  )}
                </FormControl>
                <FormControl>
                  <FormLabel>Description</FormLabel>
                  <Input
                    name="description"
                    placeholder="Entrez la description du produit"
                    variant="outlined"
                    sx={{ bgcolor: 'background.body' }}
                    value={formik.values.description}
                    onChange={formik.handleChange}
                  />
                </FormControl>
                <FormControl>
                  <FormLabel>Image du produit</FormLabel>
                  <Input
                    type="file"
                    accept="image/*"
                    onChange={handleImageChange}
                  />
                </FormControl>
                <FormControl required error={formik.touched.codeBarre && Boolean(formik.errors.codeBarre)}>
                  <FormLabel>Code barre</FormLabel>
                  <Input
                    name="codeBarre"
                    placeholder="Scanner ou entrer le code barre"
                    variant="outlined"
                    sx={{ bgcolor: 'background.body' }}
                    value={formik.values.codeBarre}
                    onChange={formik.handleChange}
                    onBlur={formik.handleBlur}
                  />
                  {formik.touched.codeBarre && formik.errors.codeBarre && (
                    <Typography color="danger" level="body-sm">{formik.errors.codeBarre}</Typography>
                  )}
                </FormControl>
                <FormControl required error={formik.touched.reference && Boolean(formik.errors.reference)}>
                  <FormLabel>Référence</FormLabel>
                  <Input
                    name="reference"
                    placeholder="Entrer la référence du produit"
                    variant="outlined"
                    sx={{ bgcolor: 'background.body' }}
                    value={formik.values.reference}
                    onChange={formik.handleChange}
                    onBlur={formik.handleBlur}
                  />
                  {formik.touched.reference && formik.errors.reference && (
                    <Typography color="danger" level="body-sm">{formik.errors.reference}</Typography>
                  )}
                </FormControl>
                <FormControl required error={formik.touched.prix && Boolean(formik.errors.prix)}>
                  <FormLabel>Prix</FormLabel>
                  <Input
                    name="prix"
                    type="number"
                    placeholder="Entrer le prix du produit"
                    variant="outlined"
                    sx={{ bgcolor: 'background.body' }}
                    value={formik.values.prix}
                    min={0}
                    onChange={formik.handleChange}
                    onBlur={formik.handleBlur}
                  />
                  {formik.touched.prix && formik.errors.prix && (
                    <Typography color="danger" level="body-sm">{formik.errors.prix}</Typography>
                  )}
                </FormControl>

                <Box sx={{ display: 'flex', gap: 2 }}>
                  <FormControl required sx={{ flex: 1 }} error={formik.touched.categorieId && Boolean(formik.errors.categorieId)}>
                    <FormLabel>Catégorie</FormLabel>
                    <Box sx={{ display: 'flex', gap: 1 }}>
                      <Select
                        name="categorieId"
                        placeholder="Sélectionnez une catégorie"
                        color="primary"
                        variant="soft"
                        size="lg"
                        value={formik.values.categorieId}
                        onChange={(_, value) => {
                          formik.setFieldValue('categorieId', value);
                          formik.setFieldValue('sousCategorieId', '');
                          fetchSousCategories(value);
                        }}
                        sx={{ flex: 1 }}
                      >
                        {categories.map((cat) => (
                          <Option key={cat.id} value={cat.id}>
                            {cat.name}
                          </Option>
                        ))}
                      </Select>
                      <Input
                        placeholder="Nouvelle catégorie"
                        value={newCategoryName}
                        onChange={e => setNewCategoryName(e.target.value)}
                        sx={{ minWidth: 120 }}
                        size="sm"
                      />
                      <Button
                        size="sm"
                        loading={creatingCategory}
                        onClick={handleCreateCategory}
                        disabled={!newCategoryName.trim()}
                      >
                        +
                      </Button>
                    </Box>
                    {formik.touched.categorieId && formik.errors.categorieId && (
                      <Typography color="danger" level="body-sm">{formik.errors.categorieId}</Typography>
                    )}
                  </FormControl>

                  <FormControl sx={{ flex: 1 }} error={formik.touched.quantitetheo && Boolean(formik.errors.quantitetheo)}>
                    <FormLabel>Quantité</FormLabel>
                    <Input
                      name="quantitetheo"
                      type="number"
                      sx={{ bgcolor: 'background.body' }}
                      value={formik.values.quantitetheo}
                      min={0}
                      onChange={formik.handleChange}
                      onBlur={formik.handleBlur}
                    />
                    {formik.touched.quantitetheo && formik.errors.quantitetheo && (
                      <Typography color="danger" level="body-sm">{formik.errors.quantitetheo}</Typography>
                    )}
                  </FormControl>
                </Box>

                {formik.values.categorieId && (
                  <FormControl>
                    <FormLabel>Sous-catégorie</FormLabel>
                    <Box sx={{ display: 'flex', gap: 1 }}>
                      <Select
                        name="sousCategorieId"
                        value={formik.values.sousCategorieId || ''}
                        onChange={(_, value) => formik.setFieldValue('sousCategorieId', value)}
                        required
                        sx={{ flex: 1 }}
                      >
                        {Array.isArray(sousCategories) && sousCategories.map((scat) => (
                          <Option key={scat.id} value={scat.id}>
                            {scat.name}
                          </Option>
                        ))}
                      </Select>
                      <Input
                        placeholder="Nouvelle sous-catégorie"
                        value={newSubCategoryName}
                        onChange={e => setNewSubCategoryName(e.target.value)}
                        sx={{ minWidth: 120 }}
                        size="sm"
                      />
                      <Button
                        size="sm"
                        loading={creatingSubCategory}
                        onClick={handleCreateSubCategory}
                        disabled={!newSubCategoryName.trim()}
                      >
                        +
                      </Button>
                    </Box>
                  </FormControl>
                )}

                <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end', mt: 2 }}>
                  <Button 
                    variant="outlined" 
                    color="neutral"
                    startDecorator={<CloudUploadIcon />}
                    onClick={() => setShowImportModal(true)}
                  >
                    Import en masse
                  </Button>
                  <Button 
                    variant="outlined" 
                    color="neutral" 
                    onClick={() => {
                      formik.resetForm();
                      setImageFile(null);
                    }}
                  >
                    Annuler
                  </Button>
                  <Button
                    type="submit"
                    loading={loading}
                    size="lg"
                    color="primary"
                    onClick={formik.handleSubmit}
                    disabled={loading || !formik.isValid || !formik.dirty}
                  >
                    Ajouter le produit
                  </Button>
                </Box>
              </Stack>
            )}
          </Box>
        </Card>
      </Box>
      <Modal open={showImportModal} onClose={() => setShowImportModal(false)}>
  <ModalDialog
    aria-labelledby="import-modal"
    size="md"
    sx={{
      maxWidth: 500,
      borderRadius: 'md',
      p: 3,
      boxShadow: 'lg',
    }}
  >
    <Typography level="h4" sx={{ mb: 2 }}>
      Import en masse de produits
    </Typography>
    
    <Box
      onDragEnter={handleDrag}
      onDragLeave={handleDrag}
      onDragOver={handleDrag}
      onDrop={handleDrop}
      sx={{
        border: '2px dashed',
        borderColor: dragActive ? 'primary.main' : 'neutral.outlinedBorder',
        borderRadius: 'md',
        p: 3,
        textAlign: 'center',
        cursor: 'pointer',
        '&:hover': {
          borderColor: 'primary.main',
          bgcolor: 'action.hover',
        },
      }}
    >
      <input
        type="file"
        accept=".xlsx,.csv,.json"
        onChange={(e) => {
          const file = e.target.files[0];
          if (file && file.name.endsWith('.csv')) {
            handleCSVImport(file);
          } else if (file && file.name.endsWith('.json')) {
            handleFileImport(file);
          } else if (file && (file.name.endsWith('.xlsx') || file.name.endsWith('.xls'))) {
            handleExcelImport(file);
          }
        }}
        style={{ display: 'none' }}
        id="file-upload"
      />
      <label htmlFor="file-upload" style={{ cursor: 'pointer' }}>
        <CloudUploadIcon sx={{ fontSize: 40, mb: 1, color: 'primary.main' }} />
        <Typography level="body-lg" sx={{ mb: 1 }}>
          Glissez ou cliquez pour importer vos produits
        </Typography>
        <Typography level="body-sm" sx={{ color: 'text.secondary' }}>
          Formats supportés: XLSX, CSV, JSON
        </Typography>
      </label>
    </Box>

    {importStatus.loading && (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
        <CircularProgress size="sm" />
      </Box>
    )}

    {importStatus.error && (
      <Alert color="danger" sx={{ mt: 2 }}>
        {importStatus.error}
      </Alert>
    )}

    {importStatus.success && (
      <Alert color="success" sx={{ mt: 2 }}>
        {importStatus.success}
      </Alert>
    )}
  </ModalDialog>
</Modal>
    </Box>
  );
};

export default AjouterProduit;