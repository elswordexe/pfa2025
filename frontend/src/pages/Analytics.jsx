import React, { useState, useEffect } from 'react';
import { 
  Box, Card, Grid, Typography, Select, 
  Option, Table, Button, CircularProgress
} from '@mui/joy';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, 
  PieChart, Pie, Cell, LineChart, Line, ResponsiveContainer 
} from 'recharts';
import Sidebarsuper from '../components/Sidebar';
import axios from 'axios';

const Analytics = () => {
  const [timeframe, setTimeframe] = useState('month');
  const [loading, setLoading] = useState(true);
  const [analyticsData, setAnalyticsData] = useState({
    globalStats: null,
    ecartsStats: null,
    performanceStats: null,
    timeStats: null
  });

  const fetchAnalytics = async (start = null, end = null) => {
    setLoading(true);
    try {

      const [globalRes, ecartsRes, performanceRes, timeRes] = await Promise.all([
        axios.get('http://localhost:8080/api/analytics/global-stats'),
        axios.get('http://localhost:8080/api/analytics/ecarts-stats'),
        axios.get('http://localhost:8080/api/analytics/performance-stats'),
        axios.get('http://localhost:8080/api/analytics/time-stats', {
          params: {
            startDate: start,
            endDate: end
          },
          paramsSerializer: params => {
            const searchParams = new URLSearchParams();
            if (params.startDate) searchParams.append('startDate', params.startDate);
            if (params.endDate) searchParams.append('endDate', params.endDate);
            return searchParams.toString();
          }
        })
      ]);

      setAnalyticsData({
        globalStats: globalRes.data,
        ecartsStats: ecartsRes.data,
        performanceStats: performanceRes.data,
        timeStats: timeRes.data
      });
    } catch (error) {
      console.error('Erreur lors de la récupération des analytics:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const now = new Date();
    let startDate;
    switch (timeframe) {
      case 'week':
        startDate = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
        break;
      case 'month':
        startDate = new Date(now.getTime());
        startDate.setMonth(now.getMonth() - 1);
        break;
      case 'quarter':
        startDate = new Date(now.getTime());
        startDate.setMonth(now.getMonth() - 3);
        break;
      case 'year':
        startDate = new Date(now.getTime());
        startDate.setFullYear(now.getFullYear() - 1);
        break;
      default:
        startDate = new Date(now.getTime());
        startDate.setMonth(now.getMonth() - 1);
    }
    function formatLocalDateTime(date) {
      return date.toISOString().slice(0, 19);
    }
    fetchAnalytics(formatLocalDateTime(startDate), formatLocalDateTime(new Date()));
  }, [timeframe]);

  const handleTimeframeChange = (_, value) => {
    if (value) {
      setTimeframe(value);
    }
  };

  const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042'];

  const renderGlobalStats = () => {
    if (!analyticsData.globalStats) return null;
    
    const { totalPlans, plansEnCours, plansTermines, tauxCompletion } = analyticsData.globalStats;
    
    return (
      <Grid container spacing={2}>
        <Grid xs={12} md={3}>
          <Card variant="soft">
            <Typography level="h4">{totalPlans}</Typography>
            <Typography>Plans Totaux</Typography>
          </Card>
        </Grid>
        <Grid xs={12} md={3}>
          <Card variant="soft">
            <Typography level="h4">{plansEnCours}</Typography>
            <Typography>Plans en Cours</Typography>
          </Card>
        </Grid>
        <Grid xs={12} md={3}>
          <Card variant="soft">
            <Typography level="h4">{plansTermines}</Typography>
            <Typography>Plans Terminés</Typography>
          </Card>
        </Grid>
        <Grid xs={12} md={3}>
          <Card variant="soft">
            <Typography level="h4">{tauxCompletion.toFixed(1)}%</Typography>
            <Typography>Taux de Complétion</Typography>
          </Card>
        </Grid>
      </Grid>
    );
  };

  const renderEcartsChart = () => {
    if (!analyticsData.ecartsStats) return null;

    const { ecartsByType, ecartsByStatut } = analyticsData.ecartsStats;
    const data = Object.entries(ecartsByType).map(([type, count]) => ({
      name: type,
      value: count
    }));

    return (
      <ResponsiveContainer width="100%" height={300}>
        <PieChart>
          <Pie
            data={data}
            dataKey="value"
            nameKey="name"
            cx="50%"
            cy="50%"
            outerRadius={100}
            label
          >
            {data.map((entry, index) => (
              <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
            ))}
          </Pie>
          <Tooltip />
          <Legend />
        </PieChart>
      </ResponsiveContainer>
    );
  };

  const renderPerformanceChart = () => {
    if (!analyticsData.performanceStats) return null;

    const { recomptagePlanStats } = analyticsData.performanceStats;
    const data = Object.entries(recomptagePlanStats).map(([planId, taux]) => ({
      planId: `Plan ${planId}`,
      taux: taux
    }));

    return (
      <ResponsiveContainer width="100%" height={300}>
        <BarChart data={data}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="planId" />
          <YAxis />
          <Tooltip />
          <Legend />
          <Bar dataKey="taux" name="Taux de Recomptage (%)" fill="#8884d8" />
        </BarChart>
      </ResponsiveContainer>
    );
  };

  const renderTimeStats = () => {
    if (!analyticsData.timeStats) return null;

    const { evolutionPlans, evolutionCheckups } = analyticsData.timeStats;
    const data = Object.entries(evolutionPlans).map(([date, count]) => ({
      date,
      plans: count,
      checkups: evolutionCheckups[date] || 0
    }));

    return (
      <ResponsiveContainer width="100%" height={300}>
        <LineChart data={data}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="date" />
          <YAxis />
          <Tooltip />
          <Legend />
          <Line type="monotone" dataKey="plans" name="Plans" stroke="#8884d8" />
          <Line type="monotone" dataKey="checkups" name="Contrôles" stroke="#82ca9d" />
        </LineChart>
      </ResponsiveContainer>
    );
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <CircularProgress />
      </Box>
    );
  }

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
          <Grid xs={12}>
            {renderGlobalStats()}
          </Grid>
          
          <Grid xs={12} md={6}>
            <Card>
              <Typography level="h5" sx={{ mb: 2 }}>
                Répartition des Écarts
              </Typography>
              {renderEcartsChart()}
            </Card>
          </Grid>
          
          <Grid xs={12} md={6}>
            <Card>
              <Typography level="h5" sx={{ mb: 2 }}>
                Performance des Plans
              </Typography>
              {renderPerformanceChart()}
            </Card>
          </Grid>
          
          <Grid xs={12}>
            <Card>
              <Typography level="h5" sx={{ mb: 2 }}>
                Évolution Temporelle
              </Typography>
              {renderTimeStats()}
            </Card>
          </Grid>
        </Grid>
      </Box>
    </Box>
  );
};

export default Analytics;