import React, { useState } from 'react';

export const  ZoomableImage = ({ src, alt = '' }) => {
  const [isZoomed, setIsZoomed] = useState(false);

  const handleImageClick = () => setIsZoomed(true);
  const handleClose = () => setIsZoomed(false);

  return (
    <>
      <img
        src={src}
        alt={alt}
        style={{
          width: 48,
          height: 48,
          objectFit: 'cover',
          borderRadius: 8,
          cursor: 'pointer',
        }}
        onClick={handleImageClick}
        onError={(e) => {
          e.target.onerror = null;
          e.target.src = '/placeholder.png';
        }}
      />
      {isZoomed && (
        <div
          onClick={handleClose}
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.7)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 9999,
          }}
        >
          <img
            src={src}
            alt={alt}
            style={{
              maxWidth: '90%',
              maxHeight: '90%',
              borderRadius: 12,
              boxShadow: '0 0 20px rgba(255,255,255,0.3)',
            }}
          />
        </div>
      )}
    </>
  );
};

export default ZoomableImage;
