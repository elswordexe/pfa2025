import * as XLSX from 'xlsx';
import { saveAs } from 'file-saver';

export function exportProductsToExcel(products, filename = 'produits.xlsx', validatedProducts = []) {
  const validatedMap = {};
  validatedProducts.forEach(vp => {
    const zoneId = vp.zoneId || vp.validatedZone;
    if (zoneId !== undefined && zoneId !== null) {
      validatedMap[`${vp.id}-${zoneId}`] = vp;
    }
  });

  let exportList = products;
  if (validatedProducts.length > 0) {
    exportList = validatedProducts.map(vp => {
      const full = products.find(p => p.id === vp.id);
      return full ? { ...full, ...vp, zoneId: vp.zoneId || vp.validatedZone } : { ...vp, zoneId: vp.zoneId || vp.validatedZone };
    });
  }

  const data = exportList.map(p => {
    let validated = null;
    let zoneId = p.zoneId || (p.zones && p.zones[0]?.id);
    if (zoneId !== undefined && zoneId !== null) {
      validated = validatedMap[`${p.id}-${zoneId}`];
    }
    let ecart = '-';
    if (validated) {
      const avant = validated.quantiteAvantValidation !== undefined ? validated.quantiteAvantValidation : (p.oldQuantiteAvant !== undefined ? p.oldQuantiteAvant : '');
      const validee = validated.quantiteValidee ?? validated.quantite ?? '-';
      if (validee !== '-' && avant !== '' && avant !== '-') {
        ecart = Number(validee) - Number(avant);
      }
      return {
        'Nom': p.nom,
        'Code Barre': p.codeBarre,
        'Quantité Avant Validation': avant,
        'Quantité Validée': validee,
        'Écart': ecart,
        'Statut': 'Validé',
        'Zone': zoneId || '-',
      };
    } else {
      ecart = (p.quantiteManuelle || p.quantiteScan || 0) - (p.quantitetheo || p.quantiteTheorique || 0);
      let statut = '';
      if (!isNaN(ecart)) {
        statut = ecart === 0 ? 'Conforme' : ecart > 0 ? 'Surplus' : 'Manquant';
      }
      return {
        'Nom': p.nom,
        'Code Barre': p.codeBarre,
        'Quantité Théorique': p.quantitetheo || p.quantiteTheorique || '-',
        'Quantité Manuelle': p.quantiteManuelle || '-',
        'Quantité Scan': p.quantiteScan || '-',
        'Écart': ecart,
        'Statut': statut,
        'Zone': zoneId || '-',
      };
    }
  });

  const worksheet = XLSX.utils.json_to_sheet(data, { cellStyles: true });

  const range = XLSX.utils.decode_range(worksheet['!ref']);
  for (let C = range.s.c; C <= range.e.c; ++C) {
    const cell = worksheet[XLSX.utils.encode_cell({ r: 0, c: C })];
    if (cell) {
      cell.s = {
        font: { bold: true, color: { rgb: 'FFFFFF' } },
        fill: { fgColor: { rgb: '2563EB' } },
        alignment: { horizontal: 'center' },
      };
    }
  }

  let ecartCol = Object.keys(data[0] || {}).findIndex(k => k.toLowerCase().replace(/é/g, 'e') === 'ecart');
  if (ecartCol !== -1) {
    for (let R = 1; R <= data.length; ++R) {
      const cell = worksheet[XLSX.utils.encode_cell({ r: R, c: ecartCol })];
      if (cell && cell.v !== undefined && cell.v !== '-') {
        let color = '1F2937';
        let val = cell.v;
        if (typeof val === 'string') val = Number(val.replace(',', '.'));
        if (!isNaN(val)) {
          if (val > 0) color = '16A34A';
          else if (val < 0) color = 'DC2626';
        }
        cell.s = {
          font: { bold: true, color: { rgb: color } },
          alignment: { horizontal: 'center' },
        };
      }
    }
  }
  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, worksheet, 'Produits');
  const excelBuffer = XLSX.write(workbook, { bookType: 'xlsx', type: 'array', cellStyles: true });
  saveAs(new Blob([excelBuffer], { type: 'application/octet-stream' }), filename);
}