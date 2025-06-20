import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import {
  Drawer,
  Box,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  useTheme,
  useMediaQuery
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import WindowIcon from '@mui/icons-material/Window';
import ShowChartRoundedIcon from '@mui/icons-material/ShowChartRounded';
import PeopleIcon from '@mui/icons-material/PeopleAltRounded';
import InventoryIcon from './InventoryIcon';
import MapIcon from '@mui/icons-material/MapRounded';
import CategoryIcon from '@mui/icons-material/Category';
import AddBoxIcon from '@mui/icons-material/AddBox';
import ListIcon from '@mui/icons-material/List';
import Inventory from '@mui/icons-material/Inventory';
import LocationPinIcon from '@mui/icons-material/LocationPin';
import LogoutIcon from '@mui/icons-material/Logout';
const drawerWidth = 240;

const getNavItems = (role) => {
  const commonProducts = {
    label: 'Products',
    icon: <CategoryIcon />,
    subItems: [
      { label: 'Product List', icon: <ListIcon />, path: '/produits' },
      { label: 'Add Product', icon: <AddBoxIcon />, path: '/produits/ajouter' },
    ],
  };

  switch (role) {
    case 'SUPER_ADMIN':
      return [
        { label: 'Overview', icon: <WindowIcon />, path: '/dashsuperadmin' },
        { label: 'Analytics', icon: <ShowChartRoundedIcon />, path: '/analytics' },
        { label: 'Inventory', icon: <Inventory />, path: '/inventory' },
        { label: 'Plans Management', icon: <MapIcon />, path: '/plans-management' },
        commonProducts,
        { label: 'Zones', icon: <LocationPinIcon />, path: '/zones' },
        { label: 'Clients', icon: <PeopleIcon />, path: '/clients' },
        { label: 'Users', icon: <PeopleIcon />, path: '/users' },
        { label: 'Logout', icon: <LogoutIcon />, action: 'logout' },
      ];
    case 'ADMIN_CLIENT':
      return [
        { label: 'Overview', icon: <WindowIcon />, path: '/dashboard' },
        { label: 'Analytics', icon: <ShowChartRoundedIcon />, path: '/analytics' },
        { label: 'Inventory', icon: <Inventory />, path: '/inventory' },
        { label: 'Plans', icon: <MapIcon />, path: '/plans' },
        commonProducts,
        { label: 'Zones', icon: <LocationPinIcon />, path: '/zones' },
        { label: 'Logout', icon: <LogoutIcon />, action: 'logout' },
        { label: 'Users', icon: <PeopleIcon />, path: '/users' },
      ];
    case 'AGENT_INVENTAIRE':
      return [
        { label: 'Overview', icon: <WindowIcon />, path: '/dashboard' },
        { label: 'Inventory', icon: <Inventory />, path: '/inventory' },
        { label: 'Plans', icon: <MapIcon />, path: '/plans' },
        commonProducts,
        { label: 'Zones', icon: <LocationPinIcon />, path: '/zones' },
      ];
    default:
      return [
        { label: 'Overview', icon: <WindowIcon />, path: '/dashboard' },
        { label: 'Plans', icon: <MapIcon />, path: '/plans' },
        commonProducts,
        { label: 'Logout', icon: <LogoutIcon />, action: 'logout' },
      ];
  }
};

export const Sidebar = () => {
  const [mobileOpen, setMobileOpen] = React.useState(false);
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const location = useLocation();
  const [expandedItem, setExpandedItem] = React.useState(null);
  const userRole = localStorage.getItem('userRole');
  const navItems = React.useMemo(() => getNavItems(userRole), [userRole]);

  const drawerStyle = {
    width: drawerWidth,
    flexShrink: 0,
    '& .MuiDrawer-paper': {
      width: drawerWidth,
      boxSizing: 'border-box',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      backdropFilter: 'blur(10px)',
      borderRight: '1px solid rgba(0,0,0,0.08)',
      boxShadow: '0 4px 12px 0 rgba(0,0,0,0.05)',
    },
  };
  const filteredNavItems = React.useMemo(() => {
    return navItems.filter(item => {
      if (item.label === 'Users' && userRole !== 'SUPER_ADMIN') return false;
      return true;
    });
  }, [navItems, userRole]);

  const drawer = (
    <Box sx={{ 
      p: 2,
      height: '100%',
      display: 'flex',
      flexDirection: 'column',
    }}>
      <Box sx={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center',
        mb: 4,
        pt: 2 
      }}>
        <InventoryIcon 
          style={{ 
            width: '64px', 
            height: '64px',
            color: userRole==='SUPER_ADMIN' ? '#000000' : '#1976d2'
          }} 
        />
      </Box>
      
<List>
  {filteredNavItems.map((item) => (
    <React.Fragment key={item.label}>
      <ListItem disablePadding>  
        <ListItemButton
          component={item.subItems || item.action==='logout' ? 'div' : Link}
          to={item.subItems || item.action==='logout' ? undefined : item.path}
          selected={!item.subItems && location.pathname === item.path}
          onClick={() => {
            if(item.action==='logout'){
              localStorage.clear();
              window.location.href='/login';
              return;
            }
            if (item.subItems) {
              setExpandedItem(expandedItem === item.label ? null : item.label);
            }
          }}
          sx={{
            borderRadius: 1,
            mb: 0.5,
            color: 'text.secondary',
            '&.Mui-selected': {
              bgcolor: 'rgba(25, 118, 210, 0.08)',
              color: 'primary.main',
              '& .MuiListItemIcon-root': {
                color: 'primary.main',
              }
            },
            '&:hover': {
              bgcolor: 'rgba(25, 118, 210, 0.04)',
            }
          }}
        >
          <ListItemIcon sx={{ color: 'inherit', minWidth: 40 }}>
            {item.icon}
          </ListItemIcon>
          <ListItemText 
            primary={item.label}
            sx={{
              '& .MuiTypography-root': {
                fontWeight: 500,
                fontSize: '0.875rem'
              }
            }}
          />
        </ListItemButton>
      </ListItem>

      {item.subItems && expandedItem === item.label && (
        <List component="div" disablePadding>
          {item.subItems.map((subItem) => (
            <ListItemButton
              key={subItem.label}
              component={Link}
              to={subItem.path}
              selected={location.pathname === subItem.path}
              sx={{ pl: 4 }}
            >
              <ListItemIcon>{subItem.icon}</ListItemIcon>
              <ListItemText primary={subItem.label} />
            </ListItemButton>
          ))}
        </List>
      )}
    </React.Fragment>
  ))}
</List>


    </Box>
  );

  return (
    <>
      {isMobile ? (
        <>
          <IconButton
            color="inherit"
            edge="start"
            onClick={() => setMobileOpen(!mobileOpen)}
            sx={{ position: 'fixed', top: 10, left: 10, zIndex: 1201 }}
          >
            <MenuIcon />
          </IconButton>
          <Drawer
            variant="temporary"
            open={mobileOpen}
            onClose={() => setMobileOpen(false)}
            ModalProps={{ keepMounted: true }}
            sx={drawerStyle}
          >
            {drawer}
          </Drawer>
        </>
      ) : (
        <Drawer
          variant="permanent"
          open
          sx={drawerStyle}
        >
          {drawer}
        </Drawer>
      )}
    </>
  );
};

export default Sidebar;
