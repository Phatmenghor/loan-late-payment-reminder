CREATE OR REPLACE VIEW STG.VIEW_LOAN_LATE_REMINDER AS
SELECT
    a.reportdate,
    a.customerid,
    b.mbapp_phone,
    a.arrangement_id,
    a.daydue
FROM sg_grid_summary a
INNER JOIN sg_customers b ON a.customerid = b.customerid
WHERE TRUNC(a.reportdate) = TRUNC(SYSDATE) - 1
  AND a.daydue BETWEEN 3 AND 30
  AND b.mbapp_phone IS NOT NULL
ORDER BY a.reportdate DESC, a.customerid;

