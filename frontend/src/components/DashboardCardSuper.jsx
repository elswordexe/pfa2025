import * as React from 'react';
import axios from 'axios';
import Button from '@mui/joy/Button';
import Card from '@mui/joy/Card';
import CardContent from '@mui/joy/CardContent';
import CircularProgress from '@mui/joy/CircularProgress';
import Typography from '@mui/joy/Typography';
import PersonIcon from '@mui/icons-material/Person';
import MapIcon from '@mui/icons-material/Map';
import InventoryIcon from '@mui/icons-material/Inventory';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import Box from '@mui/joy/Box';

function DataCard({ title, icon: Icon, endpoint }) {
  const [count, setCount] = React.useState(null);
  const [loading, setLoading] = React.useState(true);

  React.useEffect(() => {
    axios.get(endpoint)
      .then(res => {
        setCount(res.data ?? 0);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  }, [endpoint]);

  return (
    <Card
      variant="solid"
      color="primary"
      invertedColors
      sx={{
        flex: '1 1 260px',
        minWidth: { xs: '100%', sm: 240 },
        maxWidth: '100%',
        boxShadow: 'md',
        borderRadius: 'lg',
        bgcolor: 'black',
        '&:hover': {
          bgcolor: 'black.700',
        }
      }}
    >
      <CardContent orientation="horizontal" sx={{ gap: 2 }}>
        <CircularProgress
          size="lg"
          determinate
          value={loading ? 100 : 100}
          thickness={8}
          sx={{ 
            '--CircularProgress-trackColor': 'rgba(255 255 255 / 0.4)',
            '--CircularProgress-progressColor': 'white'
          }}
        >
          <Icon sx={{ fontSize: 40, color: 'white' }} />
        </CircularProgress>
        <CardContent sx={{ padding: 0 }}>
          <Typography level="body-md" sx={{ color: 'rgba(255 255 255 / 0.85)' }}>
            {title}
          </Typography>
          <Typography level="h2" sx={{ color: 'white', fontWeight: 'xl' }}>
            {loading ? 'Loading...' : count}
          </Typography>
        </CardContent>
      </CardContent>
    </Card>
  );
}

export default function DashboardCardSuper() {
  return (
    <Box
      sx={{
        display: 'flex',
        flexWrap: 'wrap',
        gap: 2,
        justifyContent: 'center',
        width: '100%',
      }}
    >
      <DataCard 
        title="Agents Inventaire" 
        icon={PersonIcon} 
        endpoint="http://localhost:8080/users/countAgentInventaire" 
      />
      <DataCard 
        title="Administrateur Clients" 
        icon={PersonIcon} 
        endpoint=" http://localhost:8080/users/countAdminClient" 
      />
      <DataCard 
        title="Zones" 
        icon={MapIcon} 
        endpoint="http://localhost:8080/Zone/count" 
      />
      <DataCard 
        title="Products" 
        icon={InventoryIcon} 
        endpoint="http://localhost:8080/produits/count" 
      />
      <DataCard 
        title="Completed Plans" 
        icon={CheckCircleIcon} 
        endpoint="http://localhost:8080/api/plans/countterminer" 
      />
    </Box>
  );
}
