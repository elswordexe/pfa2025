import { Calendar, momentLocalizer } from 'react-big-calendar';
import moment from 'moment';
import 'react-big-calendar/lib/css/react-big-calendar.css';

const localizer = momentLocalizer(moment);

const InventaireCalendar = ({ events }) => {
  return (
    <div style={{ height: 600 }}>
      <Calendar
        localizer={localizer}
        events={events}
        startAccessor="start"
        endAccessor="end"
        views={['month', 'week', 'day']}
        defaultView="month"
        eventPropGetter={(event) => ({
          style: {
            backgroundColor: event.status === 'EN_cours' ? '#2563eb' : 
                           event.status === 'Termine' ? '#16a34a' : '#6b7280'
          }
        })}
      />
    </div>
  );
};

export default InventaireCalendar;