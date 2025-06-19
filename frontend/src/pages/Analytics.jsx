import React, { useState } from 'react';
import { 
  Box, Card, Grid, Typography, Select, 
  Option, Table, Button
} from '@mui/joy';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, 
  PieChart, Pie, Cell, LineChart, Line, ResponsiveContainer 
} from 'recharts';
import Sidebarsuper from '../components/Sidebar';

const Analytics = () => {
  const mockData = {
    planStats: [
      { month: 'Jan', completedPlans: 15, totalPlans: 20 },
      { month: 'Fév', completedPlans: 18, totalPlans: 22 },
      { month: 'Mar', completedPlans: 25, totalPlans: 28 },
      { month: 'Avr', completedPlans: 22, totalPlans: 24 },
      { month: 'Mai', completedPlans: 30, totalPlans: 32 },
      { month: 'Jun', completedPlans: 28, totalPlans: 30 }
    ],
    ecartStats: {
      surplus: Array(15).fill().map((_, i) => ({
        productId: i + 1,
        quantity: Math.floor(Math.random() * 10) + 1,
        zone: `Zone ${String.fromCharCode(65 + (i % 5))}`
      })),
      manquants: Array(8).fill().map((_, i) => ({
        productId: i + 20,
        quantity: -(Math.floor(Math.random() * 10) + 1),
        zone: `Zone ${String.fromCharCode(65 + (i % 5))}`
      }))
    },
    zoneStats: [
      { zoneId: 1, name: 'Zone A', accuracy: 95, totalProducts: 150 },
      { zoneId: 2, name: 'Zone B', accuracy: 89, totalProducts: 120 },
      { zoneId: 3, name: 'Zone C', accuracy: 92, totalProducts: 200 },
      { zoneId: 4, name: 'Zone D', accuracy: 85, totalProducts: 180 },
      { zoneId: 5, name: 'Zone E', accuracy: 90, totalProducts: 160 }
    ],
    productStats: Array(10).fill().map((_, i) => ({
      productId: i + 1,
      name: `Produit ${i + 1}`,
      zone: `Zone ${String.fromCharCode(65 + (i % 5))}`,
      quantityTheoric: 100 + (i * 10),
      quantityReal: 100 + (i * 10) + (Math.floor(Math.random() * 21) - 10),
      lastInventoryDate: new Date(2025, 5, Math.floor(Math.random() * 30) + 1)
    })),
    historicalData: Array(12).fill().map((_, i) => ({
      date: `2025-${(i + 1).toString().padStart(2, '0')}`,
      discrepancies: Math.floor(Math.random() * 50) + 20
    }))
  };

  const generateTimeframeData = (timeframe) => {
    const today = new Date();
    let data = {
      planStats: [],
      historicalData: [],
      // Keep other stats relatively stable
      zoneStats: mockData.zoneStats,
      ecartStats: mockData.ecartStats,
      productStats: mockData.productStats,
    };

    switch (timeframe) {
      case 'week':
        // Generate last 7 days data
        for (let i = 6; i >= 0; i--) {
          const date = new Date(today);
          date.setDate(date.getDate() - i);
          data.planStats.push({
            month: date.toLocaleDateString('fr-FR', { weekday: 'short' }),
            completedPlans: Math.floor(Math.random() * 5) + 3,
            totalPlans: Math.floor(Math.random() * 3) + 5
          });
          data.historicalData.push({
            date: date.toISOString().split('T')[0],
            discrepancies: Math.floor(Math.random() * 20) + 5
          });
        }
        break;

      case 'month':
        // Generate last 30 days data
        for (let i = 29; i >= 0; i--) {
          const date = new Date(today);
          date.setDate(date.getDate() - i);
          data.planStats.push({
            month: `${date.getDate()}/${date.getMonth() + 1}`,
            completedPlans: Math.floor(Math.random() * 8) + 5,
            totalPlans: Math.floor(Math.random() * 5) + 8
          });
          data.historicalData.push({
            date: date.toISOString().split('T')[0],
            discrepancies: Math.floor(Math.random() * 30) + 10
          });
        }
        break;

      case 'quarter':
        // Generate last 3 months data by weeks
        for (let i = 11; i >= 0; i--) {
          const date = new Date(today);
          date.setDate(date.getDate() - (i * 7));
          data.planStats.push({
            month: `S${Math.floor(i/4) + 1}-${Math.floor(i%4) + 1}`,
            completedPlans: Math.floor(Math.random() * 15) + 10,
            totalPlans: Math.floor(Math.random() * 8) + 15
          });
          data.historicalData.push({
            date: date.toISOString().split('T')[0],
            discrepancies: Math.floor(Math.random() * 40) + 15
          });
        }
        break;

      case 'year':
        // Use existing monthly data
        data.planStats = mockData.planStats;
        data.historicalData = mockData.historicalData;
        break;

      default:
        data = mockData;
    }

    return data;
  };

  const [timeframe, setTimeframe] = useState('month');
  const [currentData, setCurrentData] = useState(generateTimeframeData('month'));

  // Update data when timeframe changes
  const handleTimeframeChange = (_, value) => {
    if (value) {
      setTimeframe(value);
      setCurrentData(generateTimeframeData(value));
    }
  };

  const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042'];

  const renderInventoryChart = () => (
    <ResponsiveContainer width="100%" height={300}>
      <BarChart data={currentData.planStats}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="month" />
        <YAxis />
        <Tooltip />
        <Legend />
        <Bar dataKey="completedPlans" fill="#82ca9d" name="Plans Complétés" />
        <Bar dataKey="totalPlans" fill="#8884d8" name="Total Plans" />
      </BarChart>
    </ResponsiveContainer>
  );

  const renderEcartsChart = () => {
    const ECART_COLORS = {
      surplus: '#4CAF50',  // Bright green
      manquants: '#FF5252' // Bright red
    };

    return (
      <ResponsiveContainer width="100%" height={300}>
        <PieChart>
          <Pie
            data={[
              { 
                name: 'Surplus', 
                value: currentData.ecartStats.surplus?.length || 0
              },
              { 
                name: 'Manquants', 
                value: currentData.ecartStats.manquants?.length || 0
              }
            ]}
            dataKey="value"
            nameKey="name"
            cx="50%"
            cy="50%"
            outerRadius={100}
            label
          >
            <Cell fill={ECART_COLORS.surplus} />
            <Cell fill={ECART_COLORS.manquants} />
          </Pie>
          <Tooltip />
          <Legend />
        </PieChart>
      </ResponsiveContainer>
    );
  };

  const renderZonePerformance = () => (
    <ResponsiveContainer width="100%" height={300}>
      <BarChart data={currentData.zoneStats}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="name" />
        <YAxis />
        <Tooltip />
        <Bar dataKey="accuracy" name="Précision (%)" fill="#8884d8" />
      </BarChart>
    </ResponsiveContainer>
  );

  const renderDetailedReport = () => (
    <Table>
      <thead>
        <tr>
          <th>Produit</th>
          <th>Zone</th>
          <th>Écart</th>
          <th>Date dernier inventaire</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {currentData.productStats.map((product) => (
          <tr key={product.productId}>
            <td>{product.name}</td>
            <td>{product.zone}</td>
            <td className={
              product.quantityTheoric - product.quantityReal > 0 
                ? 'text-red-500' 
                : 'text-green-500'
            }>
              {product.quantityTheoric - product.quantityReal}
            </td>
            <td>{new Date(product.lastInventoryDate).toLocaleDateString()}</td>
            <td>
              <Button
                size="sm"
                variant="soft"
                color="primary"
                onClick={() => handleViewDetails(product.productId)}
              >
                Détails
              </Button>
            </td>
          </tr>
        ))}
      </tbody>
    </Table>
  );

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.surface' }}>
      <Sidebarsuper />
      <Box component="main" sx={{ flexGrow: 1, p: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
          <Typography level="h3" color="primary">Analyses et Rapports</Typography>
          <Select
            value={timeframe}
            onChange={handleTimeframeChange}
            sx={{ width: 150 }}
          >
            <Option value="week">7 derniers jours</Option>
            <Option value="month">30 derniers jours</Option>
            <Option value="quarter">Ce trimestre</Option>
            <Option value="year">Cette année</Option>
          </Select>
        </Box>

        <Grid container spacing={3}>
          {/* Écarts par Type */}
          <Grid xs={12} md={6}>
            <Card>
              <Typography level="h5" sx={{ mb: 2 }}>
                Écarts par Type
              </Typography>
              {renderEcartsChart()}
            </Card>
          </Grid>

          {/* Performance par Zone */}
          <Grid xs={12} md={6}>
            <Card>
              <Typography level="h5" sx={{ mb: 2 }}>
                Performance par Zone
              </Typography>
              {renderZonePerformance()}
            </Card>
          </Grid>

          {/* Tendances Historiques */}
          <Grid xs={12}>
            <Card>
              <Typography level="h5" sx={{ mb: 2 }}>
                Tendances des Écarts
              </Typography>
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={currentData.historicalData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="date" />
                  <YAxis />
                  <Tooltip />
                  <Line type="monotone" dataKey="discrepancies" stroke="#82ca9d" />
                </LineChart>
              </ResponsiveContainer>
            </Card>
          </Grid>

          {/* Tableau Détaillé */}
          <Grid xs={12}>
            <Card>
              <Typography level="h5" sx={{ mb: 2 }}>
                Rapport Détaillé
              </Typography>
              {renderDetailedReport()}
            </Card>
          </Grid>
        </Grid>
      </Box>
    </Box>
  );
};

export default Analytics;