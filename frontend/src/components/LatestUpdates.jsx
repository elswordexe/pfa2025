import React, { useEffect, useState } from 'react';
import Box from '@mui/joy/Box';
import Typography from '@mui/joy/Typography';
import axios from 'axios';

function LatestList({ title, items }) {
  return (
    <Box
      sx={{
        flex: '1 1 300px',
        bgcolor: 'white',
        borderRadius: 4,
        border: '1px solid #3675f4',
        boxShadow: '0 4px 20px rgba(244, 67, 54, 0.1)',
        p: 2.5,
        minWidth: 260,
        maxHeight: 400,
        overflowY: 'auto',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      <Typography
        level="h6"
        sx={{
          mb: 2,
          fontWeight: 'bold',
          color: '#3675f4',
          textTransform: 'uppercase',
          letterSpacing: 1,
        }}
      >
        {title}
      </Typography>

      <Box
        component="ul"
        sx={{
          listStyle: 'none',
          m: 0,
          p: 0,
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          gap: 1,
        }}
      >
        {items.map(({ id, name, nom, date, imageUrl, imageData }) => {
          const imageSrc = imageData
            ? `data:image/jpeg;base64,${imageData}`
            : imageUrl;

          return (
            <li
              key={id}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '12px',
                padding: '10px 12px',
                borderBottom: '1px solid #f7f7f7',
                borderRadius: '10px',
                backgroundColor: '#fff',
                transition: 'box-shadow 0.3s, transform 0.2s',
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.boxShadow =
                  '0 0 10px rgba(244, 67, 54, 0.15)';
                e.currentTarget.style.transform = 'scale(1.02)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.boxShadow = 'none';
                e.currentTarget.style.transform = 'scale(1)';
              }}
            >
              {imageSrc && (
                <img
                  src={imageSrc}
                  alt={name || nom}
                  style={{
                    width: 44,
                    height: 44,
                    objectFit: 'cover',
                    borderRadius: '8px',
                    border: '2px solid #f44336',
                  }}
                />
              )}
              <div>
                <strong style={{ color: '#212121' }}>{name || nom}</strong>
                <br />
                <small style={{ color: '#999' }}>
                  {new Date(date).toLocaleDateString()}
                </small>
              </div>
            </li>
          );
        })}
      </Box>
    </Box>
  );
}

export default function LatestUpdates() {
  const [userData, setUserData] = useState([]);
  const [productData, setProductData] = useState([]);
  const [planData, setPlanData] = useState([]);

  useEffect(() => {
    axios
      .get('http://localhost:8080/users/names-dates')
      .then((res) => setUserData(res.data.sort((a, b) => new Date(b.date) - new Date(a.date))))
      .catch((err) => console.error('Erreur utilisateurs :', err));
  }, []);

  useEffect(() => {
    axios
      .get('http://localhost:8080/produits/names-dates')
      .then((res) => setProductData(res.data.sort((a, b) => new Date(b.date) - new Date(a.date))))
      .catch((err) => console.error('Erreur produits :', err));
  }, []);

  useEffect(() => {
    axios
      .get('http://localhost:8080/api/plans/names-dates')
      .then((res) => setPlanData(res.data.sort((a, b) => new Date(b.date) - new Date(a.date))))
      .catch((err) => console.error('Erreur plans :', err));
  }, []);

  return (
    <Box
      sx={{
        display: 'flex',
        flexWrap: 'wrap',
        gap: 2,
        justifyContent: 'center',
        alignItems: 'flex-start',
        width: '100%',
      }}
    >
      <LatestList title="Latest Users" items={userData} />
      <LatestList title="Latest Products" items={productData} />
      <LatestList title="Latest Plans" items={planData} />
    </Box>
  );
}
