import React from 'react';

const InventoryIcon = (props) => {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 64 64"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      {...props}
    >
      <rect x="10" y="14" width="44" height="36" rx="4" ry="4" />
      <path d="M22 14v-4h20v4" />
      <path d="M18 30h28M18 38h28M18 46h28" />
    </svg>
  );
};

export default InventoryIcon;