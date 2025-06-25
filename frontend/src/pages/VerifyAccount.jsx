import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { 
  Box, 
  Typography, 
  CircularProgress, 
  Alert, 
  LinearProgress 
} from '@mui/joy';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';

const VerifyAccount = () => {
  const navigate = useNavigate();
  const [status, setStatus] = useState('loading');
  const [message, setMessage] = useState('');
  const [progress, setProgress] = useState(0);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const token = params.get('token');

    if (!token) {
      setStatus('error');
      setMessage("Aucun token fourni.");
      return;
    }
  }, [navigate]);

  return (
    <Box
      display="flex"
      flexDirection="column"
      alignItems="center"
      justifyContent="center"
      minHeight="100vh"
      bgcolor="background.default"
      p={3}
    >
      <Box
        bgcolor="white"
        borderRadius={8}
        p={4}
        boxShadow={3}
        maxWidth={400}
        width="100%"
      >
        {status === 'loading' && (
          <>
            <CircularProgress size="lg" sx={{ mb: 2 }} />
            <Typography level="h5" textAlign="center">
              Vérification en cours...
            </Typography>
          </>
        )}

        {status === 'verifying' && (
          <>
            <CircularProgress size="lg" sx={{ mb: 2 }} />
            <Typography level="h5" textAlign="center">
              {message}
            </Typography>
          </>
        )}

        {status === 'success' && (
          <>
            <Alert
              startDecorator={<CheckCircleIcon />}
              color="success"
              sx={{ mb: 2 }}
            >
              {message}
            </Alert>
            <Typography level="body-md" textAlign="center" mb={2}>
              Redirection vers la page de connexion dans {5 - Math.floor(progress/20)} secondes...
            </Typography>
            <LinearProgress 
              determinate 
              value={progress} 
              sx={{ mb: 2 }} 
              color="success"
            />
          </>
        )}

        {status === 'error' && (
          <Alert
            startDecorator={<ErrorIcon />}
            color="danger"
            sx={{ mb: 2 }}
          >
            {message}
          </Alert>
        )}
      </Box>
    </Box>
  );
};

export default VerifyAccount;
