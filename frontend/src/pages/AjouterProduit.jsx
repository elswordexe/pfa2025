import { 
  Card, FormControl, FormLabel, Input, Select, Button, 
  Typography, Stack, Option, CircularProgress, Alert, Box,
  Modal, ModalDialog
} from '@mui/joy';
import { useState, useEffect } from 'react';
import Sidebarsuper from '../components/Sidebar';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import axios from 'axios';

const AjouterProduit = () => {
  const [dragActive, setDragActive] = useState(false);
  const [importStatus, setImportStatus] = useState({ loading: false, error: '', success: '' });
  const [showImportModal, setShowImportModal] = useState(false);
  const [product, setProduct] = useState({
    nom: '',
    description: '',
    codeBarre: '',
    quantiteTheorique: 0,
    categorieId: '',
    sousCategorieId: ''
  });

  const [categories, setCategories] = useState([]);
  const [sousCategories, setSousCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchCategories = async () => {
      try {
        setLoading(true);
        const response = await fetch('http://localhost:8080/api/categories');
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        const data = await response.json();
        
        // Validate the data structure
        if (!Array.isArray(data)) {
          console.error('Received data:', data);
          throw new Error('Les données reçues ne sont pas un tableau');
        }

        // Validate each category object
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

  // 1. Update the fetchSousCategories function to handle null/undefined categoryId
  const fetchSousCategories = async (categorieId) => {
    if (!categorieId) {
      setSousCategories([]);
      return;
    }

    try {
      const response = await fetch(`http://localhost:8080/api/categories/${categorieId}/sous-categories`);
      if (!response.ok) {
        throw new Error('Failed to fetch sub-categories');
      }
      const data = await response.json();
      setSousCategories(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Error fetching sub-categories:', error);
      setSousCategories([]);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const response = await fetch('http://localhost:8080/produits', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(product)
      });
      if (response.ok) {
        alert('Produit ajouté avec succès');
        setProduct({
          nom: '',
          description: '',
          codeBarre: '',
          quantiteTheorique: 0,
          categorieId: '',
          sousCategorieId: ''
        });
      }
    } catch (error) {
      console.error('Error adding product:', error);
    } finally {
      setLoading(false);
    }
  };
  // Update the handleFileImport function to handle the new structure

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

        // Refresh product list and close modal
        setTimeout(() => {
            setShowImportModal(false);
            // Refresh your product list here if needed
        }, 2000);

    } catch (error) {
        console.error('Import error:', error);
        setImportStatus({
            loading: false,
            error: error.response?.data || 'Erreur lors de l\'import'
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
                <FormControl required>
                  <FormLabel>Nom du produit</FormLabel>
                  <Input
                    placeholder="Entrez le nom du produit"
                    variant="outlined"
                    sx={{ bgcolor: 'background.body' }}
                    value={product.nom}
                    onChange={(e) => setProduct({...product, nom: e.target.value})}
                  />
                </FormControl>

                <FormControl required>
                  <FormLabel>Code barre</FormLabel>
                  <Input
                    placeholder="Scanner ou entrer le code barre"
                    variant="outlined"
                    sx={{ bgcolor: 'background.body' }}
                    value={product.codeBarre}
                    onChange={(e) => setProduct({...product, codeBarre: e.target.value})}
                  />
                </FormControl>

                <Box sx={{ display: 'flex', gap: 2 }}>
                  <FormControl required sx={{ flex: 1 }}>
                    <FormLabel>Catégorie</FormLabel>
                    <Select
                      placeholder="Sélectionnez une catégorie"
                      color="primary"
                      variant="soft"
                      size="lg"
                      value={product.categorieId}
                      onChange={(_, value) => {
                        setProduct({...product, categorieId: value, sousCategorieId: ''});
                        fetchSousCategories(value);
                      }}
                    >
                      {categories.map((cat) => (
                        <Option key={cat.id} value={cat.id}>
                          {cat.name}
                        </Option>
                      ))}
                    </Select>
                  </FormControl>

                  <FormControl sx={{ flex: 1 }}>
                    <FormLabel>Quantité</FormLabel>
                    <Input
                      type="number"
                      sx={{ bgcolor: 'background.body' }}
                      value={product.quantiteTheorique}
                      onChange={(e) => setProduct({...product, quantiteTheorique: Number(e.target.value)})}
                    />
                  </FormControl>
                </Box>

                {product.categorieId && (
                  <FormControl>
                    <FormLabel>Sous-catégorie</FormLabel>
                    <Select
                      value={product.sousCategorieId || ''}
                      onChange={(_, value) => setProduct({...product, sousCategorieId: value})}
                      required
                    >
                      {Array.isArray(sousCategories) && sousCategories.map((scat) => (
                        <Option key={scat.id} value={scat.id}>
                          {scat.name}
                        </Option>
                      ))}
                    </Select>
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
                      setProduct({
                        nom: '',
                        description: '',
                        codeBarre: '',
                        quantiteTheorique: 0,
                        categorieId: '',
                        sousCategorieId: ''
                      });
                    }}
                  >
                    Annuler
                  </Button>
                  <Button
                    type="submit"
                    loading={loading}
                    size="lg"
                    color="primary"
                    onClick={handleSubmit}
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
        onChange={(e) => handleFileImport(e.target.files[0])}
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