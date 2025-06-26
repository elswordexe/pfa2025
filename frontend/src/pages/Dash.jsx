import React from 'react';
import Box from '@mui/joy/Box';
import Typography from '@mui/joy/Typography';
import Button from '@mui/joy/Button';
import ChevronRightRoundedIcon from '@mui/icons-material/ChevronRightRounded';
import HomeRoundedIcon from '@mui/icons-material/HomeRounded';
import Breadcrumbs from '@mui/joy/Breadcrumbs';
import PlanStatusChart from '../components/PlanStatusChart';
import Sidebarsuper from '../components/Sidebar';
import DashboardCards from '../components/DashboardCards';
import LatestUpdates from '../components/LatestUpdates';

export default function Dash() {
  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', flexDirection: { xs: 'column', md: 'row' } }}>
      <Sidebarsuper />

      <Box
        component="main"
        sx={{
          flex: 1,
          px: { xs: 2, md: 6 },
          pt: { xs: 'calc(12px + 56px)', md: 3 },
          pb: { xs: 2, md: 3 },
          display: 'flex',
          flexDirection: 'column',
          gap: 2,
          minWidth: 0,
          height: { md: '100vh' },
          overflowY: 'auto',
        }}
      >
       
        <Box sx={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap' }}>
          <Breadcrumbs
            size="sm"
            aria-label="breadcrumbs"
            separator={<ChevronRightRoundedIcon fontSize="small" />}
            sx={{ pl: 0 }}
          >
            <a href="#" style={{ color: 'inherit', textDecoration: 'none' }} aria-label="Home">
              <HomeRoundedIcon />
            </a>
            <a href="#" style={{ fontSize: 12, fontWeight: 500, color: 'inherit', textDecoration: 'none' }}>
              Dashboard
            </a>
          </Breadcrumbs>
        </Box>

      
        <Box
          sx={{
            display: 'flex',
            mb: 1,
            gap: 1,
            flexDirection: { xs: 'column', sm: 'row' },
            alignItems: { xs: 'start', sm: 'center' },
            flexWrap: 'wrap',
            justifyContent: 'space-between',
          }}
        >
          <Typography level="h2" component="h1">
            Dashboard Overview
          </Typography>
        </Box>        <DashboardCards />

        <Box
          sx={{
            display: 'flex',
            flexDirection: { xs: 'column', md: 'row' },
            gap: 2,
            mt: 2,
            flexWrap: 'wrap',
            alignItems: 'stretch',
          }}
        >
          <Box sx={{ flex: 1, minWidth: 280 }}>
            <LatestUpdates />
          </Box>
          <Box sx={{ flex: 1, minWidth: 280 }}>
            <PlanStatusChart />
          </Box>
        </Box>
      </Box>
    </Box>
  );
}
