import React from 'react';
import Box from '@mui/joy/Box';
import Typography from '@mui/joy/Typography';
import Button from '@mui/joy/Button';
import ChevronRightRoundedIcon from '@mui/icons-material/ChevronRightRounded';
import HomeRoundedIcon from '@mui/icons-material/HomeRounded';
import Breadcrumbs from '@mui/joy/Breadcrumbs';
import PlanStatusChart from './PlanStatusChart';
import Sidebarsuper from './Sidebarsuper';
import DashboardCards from './DashboardCards';
import LatestUpdates from './LatestUpdates';

export default function Dash() {
  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <Sidebarsuper />

      <Box
        component="main"
        sx={{
          flex: 1,
          px: { xs: 2, md: 6 },
          pt: { xs: 'calc(12px + 56px)', md: 3 }, // assuming header height 56px
          pb: { xs: 2, md: 3 },
          display: 'flex',
          flexDirection: 'column',
          gap: 2,
          minWidth: 0,
          height: '100vh',
          overflowY: 'auto',
        }}
      >
        {/* Breadcrumb */}
        <Box sx={{ display: 'flex', alignItems: 'center' }}>
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
            <Typography color="primary" sx={{ fontWeight: 500, fontSize: 12 }}>
              Overview
            </Typography>
          </Breadcrumbs>
        </Box>

        {/* Header + action button */}
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
        </Box>

     {/* Cards */} 
<DashboardCards />

<Box
  sx={{
    display: 'flex',
    gap: 2,
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    mt: 2,
  }}
>
  <LatestUpdates />
  <Box sx={{ flex: 1, minWidth: 280, maxWidth: 320 }}>
    <PlanStatusChart />
  </Box>
</Box>


      </Box>
    </Box>
  );
}
