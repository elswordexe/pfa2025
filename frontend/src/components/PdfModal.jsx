import React from 'react';
import InventoryPDF from './InventoryPDF';

const PdfModal = ({ open, onClose, planId, zones, planproducts, manualCheckups, scanCheckups }) => {
  if (!open) return null;
  return (
    <div className="fixed inset-0 bg-white bg-opacity-90 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-4xl overflow-hidden">
        <div className="bg-gradient-to-r from-blue-600 to-indigo-700 p-5">
          <h3 className="text-xl font-semibold text-white flex items-center gap-2">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"  d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            Exporter en PDF
          </h3>
        </div>
        <div className="p-6">
          <InventoryPDF 
            planId={planId}
            zones={zones}
            planproducts={planproducts}
            manualCheckups={manualCheckups}
            scanCheckups={scanCheckups}
          />
        </div>
        <div className="flex justify-end gap-3 p-6 bg-gray-50">
          <button onClick={onClose} className="px-5 py-2.5 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-100 transition">Retour</button>
        </div>
      </div>
    </div>
  );
};

export default PdfModal; 