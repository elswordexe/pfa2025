import React from 'react';
import logo from '../assets/logo.png';
import WindowIcon from '@mui/icons-material/Window';
import ShowChartRoundedIcon from '@mui/icons-material/ShowChartRounded';
import People from '@mui/icons-material/PeopleAltRounded';
import Setting from '@mui/icons-material/BuildRounded';
import Inventory from '@mui/icons-material/InventoryRounded';
import Map from '@mui/icons-material/MapRounded';

export const Sidebarsuper = () => {
  return (
    <div className='SideBar'>
      <div className='Logo'>
        <img src={logo} alt="Logo" className='LogoImage' />
      </div>
      <div className='Contents'>
        <ul className='MenuList'>
          <li className='MenuItem'><WindowIcon className='Icon' /><span>Overview</span></li>
          <li className='MenuItem'><ShowChartRoundedIcon className='Icon' /><span>Analytics</span></li>
          <li className='MenuItem'><Inventory className='Icon' /><span>Inventory</span></li>
          <li className='MenuItem'><Map className='Icon' /><span>Plans</span></li>
          <li className='MenuItem'><People className='Icon' /><span>Users</span></li>
          <li className='MenuItem'><Setting className='Icon' /><span>Settings</span></li>
        </ul>
      </div>
    </div>
  );
};

export default Sidebarsuper;
