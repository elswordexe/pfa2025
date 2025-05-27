import React, { useEffect, useState } from 'react';
import Box from '@mui/joy/Box';
import Typography from '@mui/joy/Typography';
import axios from 'axios';

function LatestList({ title, items }) {
  return (
    <Box
      sx={{
        flex: '1 1 30%',
        bgcolor: 'white',
        borderRadius: 4,
        border: '1px solid #f44336',
        boxShadow: '0 4px 20px rgba(244, 67, 54, 0.1)',
        p: 2.5,
        minWidth: 260,
        maxHeight: 400,
        overflowY: 'hidden',
        overflowX: 'hidden',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      <Typography
        level="h6"
        sx={{
          mb: 2,
          fontWeight: 'bold',
          color: '#f44336',
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
          overflowY: 'hidden',
          overflowX: 'hidden',
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
  const [userData3, setUserData3] = useState([]);
  const [userData2, setUserData2] = useState([]);
useEffect(() => {
  axios
    .get('http://localhost:8080/users/names-dates')
    .then((response) => {
      const sorted = response.data.sort((a, b) => new Date(b.date) - new Date(a.date));
      setUserData(sorted);
    })
    .catch((error) => {
      console.error('Erreur lors du chargement des utilisateurs:', error);
    });
}, []);
useEffect(() => {
  axios
    .get('http://localhost:8080/Produits/names-dates')
    .then((response) => {
      const sorted = response.data.sort((a, b) => new Date(b.date) - new Date(a.date));
      setUserData2(sorted);
    })
    .catch((error) => {
      console.error('Erreur lors du chargement des produits:', error);
    });}, []);
    useEffect(() => {
      axios.get('http://localhost:8080/Plans/names-dates')
        .then((response) => {
          const sorted = response.data.sort((a, b) => new Date(b.date) - new Date(a.date));
          setUserData3(sorted);
        });},[]);
  return (
    <Box
      sx={{
        display: 'flex',
        gap: 2,
        flexWrap: 'wrap',
        justifyContent: 'space-between',
      }}
    >
      <LatestList title="Latest Users" items={userData} />
      <LatestList title="Latest Products" items={userData2} />
      <LatestList title="Latest Plans" items={userData3} />
    </Box>
  );
}
