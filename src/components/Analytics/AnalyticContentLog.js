import React from 'react';

function AnalyticContentLog({ analyticsData }) {
    return (
        <>
            {Object.entries(analyticsData).map(([date, data]) => (
                <div key={date}>
                    <h4>{date}:</h4>
                    {Object.entries(data).map(([category, categoryData]) => (
                        <div key={category} className="admin-content-container">
                            <h3>{category}</h3>
                            <p>Percentage: {categoryData.Percentage}</p>
                            <p>Total Expenditure: {categoryData["Total Expenditure"]}</p>
                        </div>
                    ))}
                </div>
            ))}
        </>
    );
}

export default AnalyticContentLog;