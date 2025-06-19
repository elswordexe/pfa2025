import React, { useEffect, useState } from 'react';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js';
import { Bar } from 'react-chartjs-2';
import axios from 'axios';
import {
  Box,
  Typography,
  CircularProgress,
  Card,
  Select,
  Option,
} from '@mui/joy';

ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend);

const colors = ['#e53935', '#f44336', '#ef5350', '#e57373', '#ffcdd2', '#ff8a80'];

const ProductChart = () => {
  const [zones, setZones] = useState([]);
  const [selectedZone, setSelectedZone] = useState('');
  const [chartData, setChartData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [isEmpty, setIsEmpty] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    axios.get('http://localhost:8080/Zone/all')
      .then((res) => {
        setZones(res.data);
        if (res.data.length > 0) setSelectedZone(res.data[0].id);
        setError(null);
      })
      .catch((err) => {
        console.error("Erreur chargement des zones :", err);
        setError("Impossible de charger les zones.");
      });
  }, []);

  useEffect(() => {
    if (!selectedZone) return;

    setLoading(true);
    setError(null);

    axios.get(`http://localhost:8080/produits/byZone/${selectedZone}`)
      .then((res) => {
        const data = res.data;
        const labels = data.map(item => item.nom);
        const values = data.map(item => item.prix ?? 0);
        const allZero = values.every(v => v === 0);
        setIsEmpty(allZero);

        const dataset = allZero
          ? {
              labels: ['Aucun produit'],
              datasets: [{
                data: [1],
                backgroundColor: ['#e0e0e0'],
                borderWidth: 0,
              }],
            }
          : {
              labels,
              datasets: [{
                label: 'Prix des Produits (Dhs)',
                data: values,
                backgroundColor: colors.slice(0, values.length),
                borderRadius: 8,
              }],
            };

        setChartData(dataset);
      })
      .catch((err) => {
        console.error("Erreur chargement des produits :", err);
        setError("Impossible de charger les produits.");
        setChartData(null);
      })
      .finally(() => setLoading(false));
  }, [selectedZone]);

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: !isEmpty,
        position: 'bottom',
        labels: {
          color: '#b71c1c',
          font: { size: 12, weight: 'bold' },
        },
      },
      title: {
        display: true,
        text: 'Produits par Zone',
        font: {
          size: 18,
          weight: 'bold',
        },
        color: '#b71c1c',
      },
    },
    scales: {
      y: {
        beginAtZero: true,
        ticks: { color: '#333' },
        grid: { color: '#f0f0f0' },
      },
      x: {
        ticks: { color: '#333' },
        grid: { display: false },
      },
    },
  };

  return (
    <Card
      variant="soft"
      color="danger"
      sx={{
        width: '100%',
        maxWidth: 850,
        mx: 'auto',
        py: 3,
        px: 4,
        borderRadius: '2xl',
        boxShadow: 'lg',
        bgcolor: 'white',
      }}
    >
      <Typography level="h4" textAlign="center" color="danger" mb={2}>
         Analyse des Produits par Zone
      </Typography>

      <Select
        value={selectedZone}
        onChange={(e) => setSelectedZone(e.target.value)}
        placeholder="Sélectionner une zone"
        size="md"
        variant="soft"
        color="danger"
        disabled={zones.length === 0}
        sx={{ mb: 3, width: 260, mx: 'auto' }}
      >
        {zones.map((zone) => (
          <Option key={zone.id} value={zone.id}>
            {zone.name}
          </Option>
        ))}
      </Select>

      {loading ? (
        <Box display="flex" justifyContent="center" mt={2}>
          <CircularProgress size="lg" color="danger" />
        </Box>
      ) : error ? (
        <Typography color="danger" textAlign="center" mt={2}>
          {error}
        </Typography>
      ) : chartData ? (
        <Box sx={{ height: 350 }}>
          <Bar data={chartData} options={options} />
          {isEmpty && (
            <Typography level="body-sm" textAlign="center" mt={2} color="neutral">
              Aucun produit dans cette zone.
            </Typography>
          )}
        </Box>
      ) : (
        <Typography color="danger" textAlign="center">
          Erreur de chargement du graphique.
        </Typography>
      )}
    </Card>
  );
};

export default ProductChart;
